package com.example.sales.service.payment;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test cho logic mapping response gateway → {@link GatewayQueryResult}.
 *
 * <p>Dùng reflection để gọi static package-private method {@code parseQueryResponse}
 * trong {@link VNPayGateway} và {@link MoMoGateway}. Test này bảo vệ logic mapping
 * status/amount mà không cần mock HTTP — là phần hay thay đổi khi vendor update docs.
 */
class GatewayQueryParsingTest {

    // ─── VNPay parseQueryResponse ──────────────────────────────────────
    @Test
    void vnpay_parseQueryResponse_successWhenResponseAndTransactionStatusAreZeroZero() throws Exception {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("vnp_ResponseCode", "00");
        res.put("vnp_TransactionStatus", "00");
        res.put("vnp_TransactionNo", "14123456");
        res.put("vnp_Amount", "9900000");
        res.put("vnp_Message", "OK");

        GatewayQueryResult out = invokeVnpayParser(res);

        assertThat(out.valid()).isTrue();
        assertThat(out.status()).isEqualTo(GatewayQueryResult.Status.SUCCESS);
        assertThat(out.providerTransNo()).isEqualTo("14123456");
        assertThat(out.amountVnd()).isEqualTo(99_000L);
        assertThat(out.rawCode()).isEqualTo("00");
    }

    @Test
    void vnpay_parseQueryResponse_pendingWhenTransactionStatusIsOne() throws Exception {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("vnp_ResponseCode", "00");
        res.put("vnp_TransactionStatus", "01");
        res.put("vnp_Amount", "9900000");

        GatewayQueryResult out = invokeVnpayParser(res);

        assertThat(out.status()).isEqualTo(GatewayQueryResult.Status.PENDING);
        assertThat(out.amountVnd()).isEqualTo(99_000L);
    }

    @Test
    void vnpay_parseQueryResponse_failedWhenTransactionStatusIsTwo() throws Exception {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("vnp_ResponseCode", "00");
        res.put("vnp_TransactionStatus", "02");
        res.put("vnp_Message", "transaction failed");

        GatewayQueryResult out = invokeVnpayParser(res);

        assertThat(out.status()).isEqualTo(GatewayQueryResult.Status.FAILED);
    }

    @Test
    void vnpay_parseQueryResponse_failedWhenResponseCodeIs91_orderNotFound() throws Exception {
        // VNPay trả 91 khi không tìm thấy txn — ta coi là FAILED để admin có thể đóng.
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("vnp_ResponseCode", "91");
        res.put("vnp_Message", "Khong tim thay giao dich");

        GatewayQueryResult out = invokeVnpayParser(res);

        assertThat(out.status()).isEqualTo(GatewayQueryResult.Status.FAILED);
        assertThat(out.rawCode()).isEqualTo("91");
    }

    @Test
    void vnpay_parseQueryResponse_unknownWhenResponseCodeIsOtherError() throws Exception {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("vnp_ResponseCode", "99");
        res.put("vnp_Message", "Unknown error");

        GatewayQueryResult out = invokeVnpayParser(res);

        assertThat(out.status()).isEqualTo(GatewayQueryResult.Status.UNKNOWN);
    }

    @Test
    void vnpay_parseQueryResponse_unknownWhenResponseIsEmpty() throws Exception {
        GatewayQueryResult out = invokeVnpayParser(new LinkedHashMap<>());
        assertThat(out.status()).isEqualTo(GatewayQueryResult.Status.UNKNOWN);
        assertThat(out.valid()).isFalse();
    }

    // ─── MoMo parseQueryResponse ──────────────────────────────────────
    @Test
    void momo_parseQueryResponse_successWhenResultCodeIsZero() throws Exception {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("resultCode", 0);
        res.put("transId", 2200000001L);
        res.put("amount", 99000);
        res.put("message", "Successful.");

        GatewayQueryResult out = invokeMomoParser(res);

        assertThat(out.valid()).isTrue();
        assertThat(out.status()).isEqualTo(GatewayQueryResult.Status.SUCCESS);
        assertThat(out.providerTransNo()).isEqualTo("2200000001");
        assertThat(out.amountVnd()).isEqualTo(99_000L);
    }

    @Test
    void momo_parseQueryResponse_pendingForCode1000And7000() throws Exception {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("resultCode", 1000);
        res.put("message", "pending");
        assertThat(invokeMomoParser(res).status()).isEqualTo(GatewayQueryResult.Status.PENDING);

        res.put("resultCode", 7000);
        assertThat(invokeMomoParser(res).status()).isEqualTo(GatewayQueryResult.Status.PENDING);
    }

    @Test
    void momo_parseQueryResponse_failedForCancelledAndOtherCodes() throws Exception {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("resultCode", 1006); // user cancelled
        res.put("message", "User cancelled");
        assertThat(invokeMomoParser(res).status()).isEqualTo(GatewayQueryResult.Status.FAILED);

        res.put("resultCode", 1003); // generic failed
        assertThat(invokeMomoParser(res).status()).isEqualTo(GatewayQueryResult.Status.FAILED);
    }

    @Test
    void momo_parseQueryResponse_unknownWhenResultCodeNotNumeric() throws Exception {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("resultCode", "NOT_A_NUMBER");
        assertThat(invokeMomoParser(res).status()).isEqualTo(GatewayQueryResult.Status.UNKNOWN);
    }

    @Test
    void momo_buildQuerySignaturePayload_isInAlphabeticalOrder() {
        String payload = MoMoGateway.buildQuerySignaturePayload(
                "accessKeyXYZ", "ORDER123", "PARTNER", "REQ456");
        assertThat(payload).isEqualTo(
                "accessKey=accessKeyXYZ&orderId=ORDER123&partnerCode=PARTNER&requestId=REQ456");
    }

    // ─── helpers ──────────────────────────────────────────────────────
    private static GatewayQueryResult invokeVnpayParser(Map<String, Object> res) throws Exception {
        Method m = VNPayGateway.class.getDeclaredMethod("parseQueryResponse", Map.class);
        m.setAccessible(true);
        return (GatewayQueryResult) m.invoke(null, res);
    }

    private static GatewayQueryResult invokeMomoParser(Map<String, Object> res) throws Exception {
        Method m = MoMoGateway.class.getDeclaredMethod("parseQueryResponse", Map.class);
        m.setAccessible(true);
        return (GatewayQueryResult) m.invoke(null, res);
    }
}
