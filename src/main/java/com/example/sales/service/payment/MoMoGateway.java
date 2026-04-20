package com.example.sales.service.payment;

import com.example.sales.config.MomoProperties;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.constant.PaymentTransactionStatus;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.PaymentTransaction;
import com.example.sales.repository.PaymentTransactionRepository;
import com.example.sales.util.PaymentSignatureUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tích hợp MoMo onetime payment theo flow {@code captureWallet} (v3 endpoint).
 * <p>
 * Docs: https://developers.momo.vn/v3/docs/payment/api/wallet/onetime/
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoMoGateway implements PaymentGateway {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final MomoProperties props;
    private final PaymentTransactionRepository txnRepository;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentGatewayType type() {
        return PaymentGatewayType.MOMO;
    }

    @Override
    public PaymentInitiation initiatePayment(PaymentRequest request) {
        if (!props.isConfigured()) {
            log.error("[MoMoGateway] thiếu MOMO_PARTNER_CODE / MOMO_ACCESS_KEY / MOMO_SECRET_KEY — không thể khởi tạo.");
            throw new BusinessException(ApiCode.PAYMENT_GATEWAY_ERROR);
        }

        String orderId = "MOMO" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String requestId = UUID.randomUUID().toString().replace("-", "");
        long amount = request.getAmountVnd();
        String orderInfo = request.getDescription() != null
                ? request.getDescription() : ("Thanh toan subscription " + request.getShopId());
        String extraData = "";
        String redirectUrl = request.getReturnUrl() != null && !request.getReturnUrl().isBlank()
                ? request.getReturnUrl()
                : props.getRedirectUrl();
        String ipnUrl = props.getIpnUrl();
        String requestType = props.getRequestType() != null ? props.getRequestType() : "captureWallet";

        String rawSignature = buildInitSignaturePayload(
                props.getAccessKey(), amount, extraData, ipnUrl, orderId, orderInfo,
                props.getPartnerCode(), redirectUrl, requestId, requestType);
        String signature = PaymentSignatureUtil.hmacSha256Hex(rawSignature, props.getSecretKey());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", props.getPartnerCode());
        body.put("accessKey", props.getAccessKey());
        body.put("requestId", requestId);
        body.put("amount", amount);
        body.put("orderId", orderId);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl", ipnUrl);
        body.put("extraData", extraData);
        body.put("requestType", requestType);
        body.put("signature", signature);
        body.put("lang", "vi");

        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (Exception ex) {
            log.error("[MoMoGateway] serialize request lỗi", ex);
            throw new BusinessException(ApiCode.PAYMENT_GATEWAY_ERROR);
        }

        txnRepository.save(PaymentTransaction.builder()
                .shopId(request.getShopId())
                .ownerId(request.getOwnerId())
                .gateway(PaymentGatewayType.MOMO)
                .providerTxnRef(orderId)
                .amountVnd(request.getAmountVnd())
                .status(PaymentTransactionStatus.PENDING)
                .rawInitRequest(json)
                .build());

        String payUrl = callCreateEndpoint(json);

        log.info("[MoMoGateway] tạo giao dịch {} amount={}đ shop={} payUrl={}",
                orderId, amount, request.getShopId(), payUrl);

        return PaymentInitiation.builder()
                .gateway(PaymentGatewayType.MOMO)
                .paymentUrl(payUrl)
                .transactionId(orderId)
                .amountVnd(request.getAmountVnd())
                .build();
    }

    private String callCreateEndpoint(String jsonBody) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(props.getEndpoint() + "/create"))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(res.body(), Map.class);
            Object payUrl = parsed.get("payUrl");
            Object resultCode = parsed.get("resultCode");
            if (payUrl == null || (resultCode != null && !"0".equals(String.valueOf(resultCode)))) {
                log.error("[MoMoGateway] create thất bại status={} body={}", res.statusCode(), res.body());
                throw new BusinessException(ApiCode.PAYMENT_GATEWAY_ERROR);
            }
            return String.valueOf(payUrl);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[MoMoGateway] gọi endpoint /create lỗi", ex);
            throw new BusinessException(ApiCode.PAYMENT_GATEWAY_ERROR);
        }
    }

    /**
     * Verify IPN/redirect payload từ MoMo. Signature được build từ các field cố định
     * theo thứ tự trong docs — sai thứ tự sẽ mismatch.
     */
    public VerifyResult verifyIpn(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return VerifyResult.invalid("empty payload");
        String received = str(payload.get("signature"));
        if (received == null || received.isBlank()) return VerifyResult.invalid("missing signature");

        String raw = buildIpnSignaturePayload(
                props.getAccessKey(),
                str(payload.get("amount")),
                str(payload.get("extraData")),
                str(payload.get("message")),
                str(payload.get("orderId")),
                str(payload.get("orderInfo")),
                str(payload.get("orderType")),
                str(payload.get("partnerCode")),
                str(payload.get("payType")),
                str(payload.get("requestId")),
                str(payload.get("responseTime")),
                str(payload.get("resultCode")),
                str(payload.get("transId"))
        );
        String expected = PaymentSignatureUtil.hmacSha256Hex(raw, props.getSecretKey());
        if (!expected.equalsIgnoreCase(received)) {
            return VerifyResult.invalid("signature mismatch");
        }

        String resultCode = str(payload.get("resultCode"));
        boolean success = "0".equals(resultCode);
        long amountVnd = 0L;
        try {
            amountVnd = Long.parseLong(str(payload.get("amount")));
        } catch (Exception ignored) {
            // keep 0 — caller will treat as mismatch.
        }
        return new VerifyResult(
                true,
                success,
                resultCode,
                str(payload.get("orderId")),
                str(payload.get("transId")),
                amountVnd,
                str(payload.get("message"))
        );
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    /** Build raw string theo spec MoMo init — fixed key order, không URL encode. */
    static String buildInitSignaturePayload(String accessKey, long amount, String extraData,
                                            String ipnUrl, String orderId, String orderInfo,
                                            String partnerCode, String redirectUrl,
                                            String requestId, String requestType) {
        return "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + (extraData == null ? "" : extraData)
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;
    }

    /** Build raw string theo spec MoMo IPN — fixed key order, không URL encode. */
    public static String buildIpnSignaturePayload(String accessKey, String amount, String extraData,
                                           String message, String orderId, String orderInfo,
                                           String orderType, String partnerCode, String payType,
                                           String requestId, String responseTime,
                                           String resultCode, String transId) {
        return "accessKey=" + nn(accessKey)
                + "&amount=" + nn(amount)
                + "&extraData=" + nn(extraData)
                + "&message=" + nn(message)
                + "&orderId=" + nn(orderId)
                + "&orderInfo=" + nn(orderInfo)
                + "&orderType=" + nn(orderType)
                + "&partnerCode=" + nn(partnerCode)
                + "&payType=" + nn(payType)
                + "&requestId=" + nn(requestId)
                + "&responseTime=" + nn(responseTime)
                + "&resultCode=" + nn(resultCode)
                + "&transId=" + nn(transId);
    }

    private static String nn(String s) {
        return s == null ? "" : s;
    }

    /**
     * Gọi MoMo {@code /query} để tra cứu trạng thái thật của giao dịch đã khởi tạo.
     *
     * <p>Spec: https://developers.momo.vn/v3/docs/payment/api/wallet/query<br/>
     * Raw signature: {@code accessKey=&orderId=&partnerCode=&requestId=}
     */
    @Override
    public GatewayQueryResult queryTransaction(PaymentTransaction txn) {
        if (!props.isConfigured()) {
            return GatewayQueryResult.unknown("MoMo chưa được cấu hình");
        }
        if (txn == null || txn.getProviderTxnRef() == null) {
            return GatewayQueryResult.unknown("missing providerTxnRef");
        }

        String orderId = txn.getProviderTxnRef();
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String raw = buildQuerySignaturePayload(
                props.getAccessKey(), orderId, props.getPartnerCode(), requestId);
        String signature = PaymentSignatureUtil.hmacSha256Hex(raw, props.getSecretKey());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", props.getPartnerCode());
        body.put("requestId", requestId);
        body.put("orderId", orderId);
        body.put("signature", signature);
        body.put("lang", "vi");

        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(props.getEndpoint() + "/query"))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(res.body(), Map.class);
            return parseQueryResponse(parsed);
        } catch (Exception ex) {
            log.error("[MoMoGateway] query lỗi orderId={}: {}", orderId, ex.toString());
            return GatewayQueryResult.unknown("MoMo query error: " + ex.getMessage());
        }
    }

    private static GatewayQueryResult parseQueryResponse(Map<String, Object> parsed) {
        if (parsed == null || parsed.isEmpty()) {
            return GatewayQueryResult.unknown("empty response");
        }
        String resultCode = str(parsed.get("resultCode"));
        String providerTransNo = str(parsed.get("transId"));
        String message = str(parsed.get("message"));
        long amountVnd = 0L;
        try {
            if (parsed.get("amount") != null) {
                amountVnd = Long.parseLong(String.valueOf(parsed.get("amount")));
            }
        } catch (NumberFormatException ignored) {
            // keep 0 — caller reconciliation will flag mismatch.
        }

        // MoMo resultCode: 0 = success; 1000/7000/7002 = pending;
        // 1001/1006/1080/11/... = failed/cancelled. Ta map theo giá trị số.
        GatewayQueryResult.Status status;
        int code;
        try {
            code = Integer.parseInt(resultCode == null ? "" : resultCode);
        } catch (NumberFormatException ex) {
            return new GatewayQueryResult(true, GatewayQueryResult.Status.UNKNOWN,
                    providerTransNo, amountVnd, resultCode,
                    "MoMo resultCode không hợp lệ: " + resultCode);
        }

        if (code == 0) {
            status = GatewayQueryResult.Status.SUCCESS;
        } else if (code == 1000 || code == 7000 || code == 7002 || code == 9000) {
            // 1000 = đã tạo, đang chờ; 7000/7002 = đang xử lý; 9000 = authorize.
            status = GatewayQueryResult.Status.PENDING;
        } else {
            // Còn lại (1001, 1006, 1080, 11, 1003…) coi là FAILED.
            status = GatewayQueryResult.Status.FAILED;
        }

        return new GatewayQueryResult(true, status, providerTransNo, amountVnd,
                resultCode, message != null ? message : ("resultCode=" + resultCode));
    }

    /** Raw signature cho query API — 4 field cố định theo alphabet. */
    public static String buildQuerySignaturePayload(String accessKey, String orderId,
                                                    String partnerCode, String requestId) {
        return "accessKey=" + nn(accessKey)
                + "&orderId=" + nn(orderId)
                + "&partnerCode=" + nn(partnerCode)
                + "&requestId=" + nn(requestId);
    }

    public record VerifyResult(boolean valid,
                               boolean success,
                               String resultCode,
                               String orderId,
                               String providerTransNo,
                               long amountVnd,
                               String message) {
        public static VerifyResult invalid(String reason) {
            return new VerifyResult(false, false, null, null, null, 0L, reason);
        }
    }
}
