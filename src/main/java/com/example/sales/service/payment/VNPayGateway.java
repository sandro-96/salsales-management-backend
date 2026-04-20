package com.example.sales.service.payment;

import com.example.sales.config.VNPayProperties;
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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Tích hợp VNPay sandbox/production theo spec vpcpay v2.1.0:
 * tạo URL có chữ ký HMAC-SHA512 và verify lại query trả về / IPN GET.
 *
 * <p>Spec tham khảo: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VNPayGateway implements PaymentGateway {

    /** Format thời điểm VNPay yêu cầu (giờ Việt Nam). */
    private static final DateTimeFormatter VNP_TS =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final VNPayProperties props;
    private final PaymentTransactionRepository txnRepository;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentGatewayType type() {
        return PaymentGatewayType.VNPAY;
    }

    @Override
    public PaymentInitiation initiatePayment(PaymentRequest request) {
        if (!props.isConfigured()) {
            log.error("[VNPayGateway] thiếu cấu hình VNPAY_TMN_CODE / VNPAY_HASH_SECRET — không thể khởi tạo.");
            throw new BusinessException(ApiCode.PAYMENT_GATEWAY_ERROR);
        }

        String txnRef = "VNP" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        String createDate = now.format(VNP_TS);
        String expireDate = now.plusMinutes(Math.max(1, props.getExpireMinutes())).format(VNP_TS);

        // vnp_Amount = số tiền * 100 theo spec.
        long amountMinorUnits = request.getAmountVnd() * 100L;
        String returnUrl = request.getReturnUrl() != null && !request.getReturnUrl().isBlank()
                ? request.getReturnUrl()
                : props.getReturnUrl();

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", props.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amountMinorUnits));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", request.getDescription() != null
                ? request.getDescription() : ("Thanh toan subscription " + request.getShopId()));
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        String hashData = buildHashData(params);
        String signed = PaymentSignatureUtil.hmacSha512Hex(hashData, props.getHashSecret());
        String queryString = buildQueryString(params) + "&vnp_SecureHash=" + signed;
        String paymentUrl = props.getPayUrl() + "?" + queryString;

        txnRepository.save(PaymentTransaction.builder()
                .shopId(request.getShopId())
                .ownerId(request.getOwnerId())
                .gateway(PaymentGatewayType.VNPAY)
                .providerTxnRef(txnRef)
                .amountVnd(request.getAmountVnd())
                .status(PaymentTransactionStatus.PENDING)
                .rawInitRequest(queryString)
                .build());

        log.info("[VNPayGateway] tạo giao dịch {} amount={}đ shop={}",
                txnRef, request.getAmountVnd(), request.getShopId());

        return PaymentInitiation.builder()
                .gateway(PaymentGatewayType.VNPAY)
                .paymentUrl(paymentUrl)
                .transactionId(txnRef)
                .amountVnd(request.getAmountVnd())
                .build();
    }

    /**
     * Verify query params do VNPay redirect về (return URL) hoặc IPN GET đẩy về.
     * Chữ ký đặt trong {@code vnp_SecureHash}; các field khác đều phải tham gia hash.
     */
    public VerifyResult verifyReturn(Map<String, String> rawParams) {
        if (rawParams == null || rawParams.isEmpty()) {
            return VerifyResult.invalid("empty params");
        }
        Map<String, String> params = new LinkedHashMap<>(rawParams);
        String receivedHash = params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");
        if (receivedHash == null || receivedHash.isBlank()) {
            return VerifyResult.invalid("missing vnp_SecureHash");
        }

        Map<String, String> sorted = new TreeMap<>(params);
        String hashData = buildHashData(sorted);
        String expected = PaymentSignatureUtil.hmacSha512Hex(hashData, props.getHashSecret());
        if (!expected.equalsIgnoreCase(receivedHash)) {
            return VerifyResult.invalid("signature mismatch");
        }

        String rspCode = params.getOrDefault("vnp_ResponseCode", "");
        String txnStatus = params.getOrDefault("vnp_TransactionStatus", "");
        boolean success = "00".equals(rspCode) && "00".equals(txnStatus);

        long amountVnd = 0L;
        try {
            long amt = Long.parseLong(params.getOrDefault("vnp_Amount", "0"));
            amountVnd = amt / 100L;
        } catch (NumberFormatException ignored) {
            // keep 0 — caller will treat as mismatch.
        }
        return new VerifyResult(
                true,
                success,
                rspCode,
                params.get("vnp_TxnRef"),
                params.get("vnp_TransactionNo"),
                amountVnd,
                null
        );
    }

    /**
     * Gọi VNPay Querydr API để tra trạng thái thực tế của giao dịch.
     * <p>
     * Spec: https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr&refund.html<br/>
     * Hash data: {@code requestId|version|command|tmnCode|txnRef|transactionDate|createDate|ipAddr|orderInfo}
     * (dùng dấu {@code |}, không URL-encode).
     */
    @Override
    public GatewayQueryResult queryTransaction(com.example.sales.model.PaymentTransaction txn) {
        if (!props.isConfigured()) {
            return GatewayQueryResult.unknown("VNPay chưa được cấu hình");
        }
        if (txn == null || txn.getProviderTxnRef() == null) {
            return GatewayQueryResult.unknown("missing providerTxnRef");
        }

        String transactionDate = extractCreateDateFromInit(txn);
        if (transactionDate == null) {
            // Fallback: dùng createdAt của record (không bằng vnp_CreateDate gốc,
            // nhưng VNPay accept nếu khớp ngày trong window 14 ngày).
            LocalDateTime base = txn.getCreatedAt() != null
                    ? txn.getCreatedAt() : LocalDateTime.now(VN_ZONE);
            transactionDate = base.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(VN_ZONE).toLocalDateTime().format(VNP_TS);
        }

        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String version = "2.1.0";
        String command = "querydr";
        String txnRef = txn.getProviderTxnRef();
        String orderInfo = "Query txn " + txnRef;
        String createDate = LocalDateTime.now(VN_ZONE).format(VNP_TS);
        String ipAddr = "127.0.0.1";

        String hashData = String.join("|",
                requestId, version, command, props.getTmnCode(),
                txnRef, transactionDate, createDate, ipAddr, orderInfo);
        String secureHash = PaymentSignatureUtil.hmacSha512Hex(hashData, props.getHashSecret());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", version);
        body.put("vnp_Command", command);
        body.put("vnp_TmnCode", props.getTmnCode());
        body.put("vnp_TxnRef", txnRef);
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", ipAddr);
        body.put("vnp_SecureHash", secureHash);

        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(props.getQueryUrl()))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(res.body(), Map.class);
            return parseQueryResponse(parsed);
        } catch (Exception ex) {
            log.error("[VNPayGateway] querydr lỗi txnRef={}: {}", txnRef, ex.toString());
            return GatewayQueryResult.unknown("VNPay query error: " + ex.getMessage());
        }
    }

    /**
     * Lấy vnp_CreateDate đã gửi lúc khởi tạo (VNPay yêu cầu match khi querydr).
     * Trả về null nếu không parse được — caller sẽ fallback theo createdAt.
     */
    private static String extractCreateDateFromInit(com.example.sales.model.PaymentTransaction txn) {
        String raw = txn.getRawInitRequest();
        if (raw == null || raw.isBlank()) return null;
        for (String part : raw.split("&")) {
            int idx = part.indexOf('=');
            if (idx <= 0) continue;
            String key = part.substring(0, idx);
            if (!"vnp_CreateDate".equals(key)) continue;
            String value = part.substring(idx + 1);
            // Value có thể đã URL-encode (chỉ ký số nên thường không encode) — trả thẳng.
            return value;
        }
        return null;
    }

    private static GatewayQueryResult parseQueryResponse(Map<String, Object> parsed) {
        if (parsed == null || parsed.isEmpty()) {
            return GatewayQueryResult.unknown("empty response");
        }
        String rspCode = asString(parsed.get("vnp_ResponseCode"));
        String txnStatus = asString(parsed.get("vnp_TransactionStatus"));
        String providerTransNo = asString(parsed.get("vnp_TransactionNo"));
        String message = asString(parsed.get("vnp_Message"));

        long amountVnd = 0L;
        try {
            Object amtRaw = parsed.get("vnp_Amount");
            if (amtRaw != null) {
                long amt = Long.parseLong(String.valueOf(amtRaw));
                amountVnd = amt / 100L;
            }
        } catch (NumberFormatException ignored) {
            // keep 0
        }

        // VNPay: vnp_ResponseCode != "00" → request không thành công (tra cứu lỗi / order not found).
        if (!"00".equals(rspCode)) {
            // "91" = không tìm thấy — coi là FAILED thực sự (để admin có thể đóng txn).
            if ("91".equals(rspCode)) {
                return new GatewayQueryResult(true, GatewayQueryResult.Status.FAILED,
                        providerTransNo, amountVnd, rspCode,
                        "VNPay: không tìm thấy giao dịch (91)");
            }
            return new GatewayQueryResult(true, GatewayQueryResult.Status.UNKNOWN,
                    providerTransNo, amountVnd, rspCode,
                    "VNPay response " + rspCode + (message != null ? " - " + message : ""));
        }

        // vnp_TransactionStatus: "00" = success, "01" = đang xử lý, "02" = thất bại.
        GatewayQueryResult.Status status = switch (txnStatus == null ? "" : txnStatus) {
            case "00" -> GatewayQueryResult.Status.SUCCESS;
            case "01" -> GatewayQueryResult.Status.PENDING;
            case "02", "09", "10", "11", "12" -> GatewayQueryResult.Status.FAILED;
            default -> GatewayQueryResult.Status.UNKNOWN;
        };
        return new GatewayQueryResult(true, status, providerTransNo, amountVnd,
                txnStatus, message != null ? message : ("txnStatus=" + txnStatus));
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    /** Build chuỗi dạng {@code k1=enc(v1)&k2=enc(v2)} với key đã sort; bỏ qua value rỗng. */
    public static String buildHashData(Map<String, String> params) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) continue;
            parts.add(e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII));
        }
        Collections.sort(parts);
        return String.join("&", parts);
    }

    /** Query string để ghép vào URL — theo spec VNPay giống hệt hashData. */
    public static String buildQueryString(Map<String, String> params) {
        return buildHashData(params);
    }

    /**
     * Kết quả verify — caller dùng để quyết định có gọi
     * {@code SubscriptionService.recordPayment} hay không.
     */
    public record VerifyResult(boolean valid,
                               boolean success,
                               String responseCode,
                               String txnRef,
                               String providerTransNo,
                               long amountVnd,
                               String errorMessage) {
        public static VerifyResult invalid(String reason) {
            return new VerifyResult(false, false, null, null, null, 0L, reason);
        }
    }
}
