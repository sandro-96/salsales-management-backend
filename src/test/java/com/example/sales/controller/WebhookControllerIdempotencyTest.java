package com.example.sales.controller;

import com.example.sales.config.MomoProperties;
import com.example.sales.config.VNPayProperties;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.constant.PaymentTransactionStatus;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.PaymentTransaction;
import com.example.sales.repository.PaymentTransactionRepository;
import com.example.sales.service.SubscriptionService;
import com.example.sales.service.payment.MoMoGateway;
import com.example.sales.service.payment.VNPayGateway;
import com.example.sales.util.PaymentSignatureUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verify idempotency guarantee: IPN callback gọi lại nhiều lần cho cùng 1 txnRef
 * chỉ được phép invoke {@code SubscriptionService.recordPayment} đúng 1 lần.
 */
class WebhookControllerIdempotencyTest {

    private static final String HASH_SECRET = "TESTSECRET1234567890";
    private static final String TXN_REF = "VNPABC123";
    private static final String MOMO_ACCESS_KEY = "MOMO_TEST_ACCESS_KEY";
    private static final String MOMO_SECRET_KEY = "MOMO_TEST_SECRET_KEY";
    private static final String MOMO_PARTNER = "MOMOTESTPARTNER";
    private static final String MOMO_ORDER_ID = "MOMO_ORDER_XYZ";

    private SubscriptionService subscriptionService;
    private PaymentTransactionRepository txnRepository;
    private VNPayGateway vnPayGateway;
    private MoMoGateway moMoGateway;
    private WebhookController controller;

    @BeforeEach
    void setUp() {
        subscriptionService = mock(SubscriptionService.class);
        txnRepository = mock(PaymentTransactionRepository.class);

        VNPayProperties vprops = new VNPayProperties();
        vprops.setTmnCode("DEMO");
        vprops.setHashSecret(HASH_SECRET);
        vnPayGateway = new VNPayGateway(vprops, txnRepository, new ObjectMapper());

        MomoProperties mprops = new MomoProperties();
        mprops.setPartnerCode(MOMO_PARTNER);
        mprops.setAccessKey(MOMO_ACCESS_KEY);
        mprops.setSecretKey(MOMO_SECRET_KEY);
        moMoGateway = new MoMoGateway(mprops, txnRepository, new ObjectMapper());

        controller = new WebhookController(
                subscriptionService,
                new ObjectMapper(),
                txnRepository,
                vnPayGateway,
                moMoGateway
        );
    }

    @Test
    void vnpayIpn_callTwice_recordsPaymentOnlyOnce() {
        // Arrange: txn ở trạng thái PENDING cho lần IPN đầu tiên.
        PaymentTransaction pending = PaymentTransaction.builder()
                .shopId("shop-1")
                .ownerId("owner-1")
                .gateway(PaymentGatewayType.VNPAY)
                .providerTxnRef(TXN_REF)
                .amountVnd(99_000L)
                .status(PaymentTransactionStatus.PENDING)
                .build();
        PaymentTransaction confirmed = PaymentTransaction.builder()
                .shopId("shop-1")
                .ownerId("owner-1")
                .gateway(PaymentGatewayType.VNPAY)
                .providerTxnRef(TXN_REF)
                .amountVnd(99_000L)
                .status(PaymentTransactionStatus.SUCCESS)
                .build();
        // Lần 1 trả PENDING, lần 2 đã SUCCESS do lần 1 lưu lại.
        when(txnRepository.findByProviderTxnRef(TXN_REF))
                .thenReturn(Optional.of(pending))
                .thenReturn(Optional.of(confirmed));
        when(txnRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));

        HttpServletRequest req = buildRequestWithSignedVnpParams();

        // Act: gọi IPN 2 lần liên tiếp.
        Map<String, String> first = controller.handleVnpayIpn(req);
        Map<String, String> second = controller.handleVnpayIpn(req);

        // Assert: lần đầu confirm success, lần 2 trả code "02" = already confirmed.
        assertThat(first.get("RspCode")).isEqualTo("00");
        assertThat(second.get("RspCode")).isEqualTo("02");

        verify(subscriptionService, times(1))
                .recordPayment(eq("shop-1"), any(), eq(PaymentGatewayType.VNPAY), eq(null));

        ArgumentCaptor<PaymentTransaction> saved = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(txnRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentTransactionStatus.SUCCESS);
    }

    @Test
    void vnpayIpn_tamperedAmount_marksFailedAndDoesNotRecord() {
        PaymentTransaction pending = PaymentTransaction.builder()
                .shopId("shop-1")
                .providerTxnRef(TXN_REF)
                .amountVnd(99_000L)
                .gateway(PaymentGatewayType.VNPAY)
                .status(PaymentTransactionStatus.PENDING)
                .build();
        when(txnRepository.findByProviderTxnRef(TXN_REF)).thenReturn(Optional.of(pending));
        when(txnRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));

        // Signed params nói amount = 1000đ (vnp_Amount=100000) — không khớp txn.amountVnd=99_000đ.
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", "100000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", TXN_REF);
        String sig = PaymentSignatureUtil.hmacSha512Hex(VNPayGateway.buildHashData(params), HASH_SECRET);
        Map<String, String> all = new LinkedHashMap<>(params);
        all.put("vnp_SecureHash", sig);
        HttpServletRequest req = requestWithParams(all);

        Map<String, String> response = controller.handleVnpayIpn(req);

        assertThat(response.get("RspCode")).isEqualTo("04");
        verify(subscriptionService, never())
                .recordPayment(any(), any(), any(), any());
    }

    @Test
    void vnpayIpn_shopDeletedAfterPayment_marksFailedWithoutExtendingSubscription() {
        PaymentTransaction pending = PaymentTransaction.builder()
                .shopId("ghost-shop")
                .providerTxnRef(TXN_REF)
                .amountVnd(99_000L)
                .gateway(PaymentGatewayType.VNPAY)
                .status(PaymentTransactionStatus.PENDING)
                .build();
        when(txnRepository.findByProviderTxnRef(TXN_REF)).thenReturn(Optional.of(pending));
        when(txnRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));
        // Subscription service từ chối vì shop không còn.
        org.mockito.Mockito.doThrow(new BusinessException(ApiCode.SHOP_NOT_FOUND))
                .when(subscriptionService)
                .recordPayment(eq("ghost-shop"), any(), eq(PaymentGatewayType.VNPAY), eq(null));

        Map<String, String> response = controller.handleVnpayIpn(buildRequestWithSignedVnpParams());

        // Gateway được ack "01" để dừng retry, txn mark FAILED, không gia hạn.
        assertThat(response.get("RspCode")).isEqualTo("01");
        ArgumentCaptor<PaymentTransaction> saved = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(txnRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentTransactionStatus.FAILED);
        assertThat(saved.getValue().getFailureReason()).contains("SHOP_NOT_FOUND");
    }

    @Test
    void momoIpn_validSignature_recordsPaymentAndMarksSuccess() {
        // Arrange: txn PENDING khớp orderId + số tiền.
        PaymentTransaction pending = PaymentTransaction.builder()
                .shopId("shop-42")
                .ownerId("owner-42")
                .gateway(PaymentGatewayType.MOMO)
                .providerTxnRef(MOMO_ORDER_ID)
                .amountVnd(99_000L)
                .status(PaymentTransactionStatus.PENDING)
                .build();
        when(txnRepository.findByProviderTxnRef(MOMO_ORDER_ID))
                .thenReturn(Optional.of(pending));
        when(txnRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> payload = buildSignedMomoIpnPayload(MOMO_ORDER_ID, 99_000L, "0", "2200000001");

        // Act
        ResponseEntity<Map<String, Object>> response = controller.handleMomoIpn(payload);

        // Assert: MoMo chấp nhận (resultCode=0) + subscriptionService gọi đúng 1 lần + txn SUCCESS.
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("resultCode")).isEqualTo(0);

        verify(subscriptionService, times(1))
                .recordPayment(eq("shop-42"), eq("2200000001"), eq(PaymentGatewayType.MOMO), eq(null));

        ArgumentCaptor<PaymentTransaction> saved = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(txnRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentTransactionStatus.SUCCESS);
        assertThat(saved.getValue().getProviderTransNo()).isEqualTo("2200000001");
    }

    @Test
    void momoIpn_duplicateCallbackForAlreadySuccess_isIdempotent() {
        PaymentTransaction already = PaymentTransaction.builder()
                .shopId("shop-42")
                .gateway(PaymentGatewayType.MOMO)
                .providerTxnRef(MOMO_ORDER_ID)
                .amountVnd(99_000L)
                .status(PaymentTransactionStatus.SUCCESS)
                .build();
        when(txnRepository.findByProviderTxnRef(MOMO_ORDER_ID))
                .thenReturn(Optional.of(already));

        Map<String, Object> payload = buildSignedMomoIpnPayload(MOMO_ORDER_ID, 99_000L, "0", "2200000001");

        ResponseEntity<Map<String, Object>> response = controller.handleMomoIpn(payload);

        assertThat(response.getBody().get("message")).isEqualTo("already confirmed");
        verify(subscriptionService, never())
                .recordPayment(any(), any(), any(), any());
        verify(txnRepository, never()).save(any());
    }

    private Map<String, Object> buildSignedMomoIpnPayload(String orderId, long amount,
                                                           String resultCode, String transId) {
        String amountStr = String.valueOf(amount);
        String message = "Successful.";
        String orderInfo = "Thanh toan gia han";
        String orderType = "momo_wallet";
        String payType = "qr";
        String requestId = orderId;
        String responseTime = "1700000000000";

        String raw = MoMoGateway.buildIpnSignaturePayload(
                MOMO_ACCESS_KEY, amountStr, "", message, orderId, orderInfo,
                orderType, MOMO_PARTNER, payType, requestId, responseTime,
                resultCode, transId);
        String signature = PaymentSignatureUtil.hmacSha256Hex(raw, MOMO_SECRET_KEY);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", MOMO_PARTNER);
        payload.put("orderId", orderId);
        payload.put("requestId", requestId);
        payload.put("amount", amountStr);
        payload.put("orderInfo", orderInfo);
        payload.put("orderType", orderType);
        payload.put("transId", transId);
        payload.put("resultCode", resultCode);
        payload.put("message", message);
        payload.put("payType", payType);
        payload.put("responseTime", responseTime);
        payload.put("extraData", "");
        payload.put("signature", signature);
        return payload;
    }

    private HttpServletRequest buildRequestWithSignedVnpParams() {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", "9900000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", TXN_REF);
        params.put("vnp_TransactionNo", "14000001");
        String sig = PaymentSignatureUtil.hmacSha512Hex(VNPayGateway.buildHashData(params), HASH_SECRET);
        Map<String, String> all = new LinkedHashMap<>(params);
        all.put("vnp_SecureHash", sig);
        return requestWithParams(all);
    }

    private HttpServletRequest requestWithParams(Map<String, String> params) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        Map<String, String[]> map = new HashMap<>();
        params.forEach((k, v) -> map.put(k, new String[]{v}));
        when(req.getParameterMap()).thenReturn(map);
        return req;
    }
}
