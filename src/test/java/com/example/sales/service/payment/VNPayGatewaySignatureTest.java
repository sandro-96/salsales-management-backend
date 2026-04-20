package com.example.sales.service.payment;

import com.example.sales.config.VNPayProperties;
import com.example.sales.util.PaymentSignatureUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit test thuần (không cần Spring) cho logic build hash data + verify return.
 * Dùng fixture có sẵn secret để assert chữ ký ổn định, không regress khi refactor.
 */
class VNPayGatewaySignatureTest {

    private static final String HASH_SECRET = "TESTSECRET1234567890";

    @Test
    void buildHashData_sortsKeysAlphabetically_andEncodesValues() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Amount", "9900000");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", "DEMO");
        params.put("vnp_OrderInfo", "Thanh toan don hang");
        String hash = VNPayGateway.buildHashData(new TreeMap<>(params));
        assertThat(hash).isEqualTo(
                "vnp_Amount=9900000"
                        + "&vnp_Command=pay"
                        + "&vnp_OrderInfo=Thanh+toan+don+hang"
                        + "&vnp_TmnCode=DEMO"
                        + "&vnp_Version=2.1.0");
    }

    @Test
    void buildHashData_skipsEmptyValues() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("a", "1");
        params.put("b", "");
        params.put("c", null);
        params.put("d", "x");
        assertThat(VNPayGateway.buildHashData(new TreeMap<>(params)))
                .isEqualTo("a=1&d=x");
    }

    @Test
    void hmacSha512Hex_isDeterministic() {
        String out1 = PaymentSignatureUtil.hmacSha512Hex("hello=world&a=1", HASH_SECRET);
        String out2 = PaymentSignatureUtil.hmacSha512Hex("hello=world&a=1", HASH_SECRET);
        assertThat(out1).isEqualTo(out2).hasSize(128);
    }

    @Test
    void verifyReturn_acceptsValidSignatureAndSuccessCode() {
        VNPayProperties props = new VNPayProperties();
        props.setTmnCode("DEMO");
        props.setHashSecret(HASH_SECRET);
        VNPayGateway gw = new VNPayGateway(props, mock(com.example.sales.repository.PaymentTransactionRepository.class), new ObjectMapper());

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", "9900000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", "VNPABC123");
        params.put("vnp_TransactionNo", "14000001");
        String hashData = VNPayGateway.buildHashData(params);
        String sig = PaymentSignatureUtil.hmacSha512Hex(hashData, HASH_SECRET);
        Map<String, String> withHash = new LinkedHashMap<>(params);
        withHash.put("vnp_SecureHash", sig);

        VNPayGateway.VerifyResult result = gw.verifyReturn(withHash);

        assertThat(result.valid()).isTrue();
        assertThat(result.success()).isTrue();
        assertThat(result.txnRef()).isEqualTo("VNPABC123");
        assertThat(result.providerTransNo()).isEqualTo("14000001");
        assertThat(result.amountVnd()).isEqualTo(99_000L);
    }

    @Test
    void verifyReturn_rejectsTamperedAmount() {
        VNPayProperties props = new VNPayProperties();
        props.setTmnCode("DEMO");
        props.setHashSecret(HASH_SECRET);
        VNPayGateway gw = new VNPayGateway(props, mock(com.example.sales.repository.PaymentTransactionRepository.class), new ObjectMapper());

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", "9900000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", "VNPABC123");
        String sig = PaymentSignatureUtil.hmacSha512Hex(VNPayGateway.buildHashData(params), HASH_SECRET);
        Map<String, String> tampered = new LinkedHashMap<>(params);
        tampered.put("vnp_Amount", "100"); // attacker lowers amount
        tampered.put("vnp_SecureHash", sig);

        assertThat(gw.verifyReturn(tampered).valid()).isFalse();
    }

    @Test
    void verifyReturn_rejectsMissingHash() {
        VNPayProperties props = new VNPayProperties();
        props.setHashSecret(HASH_SECRET);
        VNPayGateway gw = new VNPayGateway(props, mock(com.example.sales.repository.PaymentTransactionRepository.class), new ObjectMapper());

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", "VNPABC123");
        assertThat(gw.verifyReturn(params).valid()).isFalse();
    }

    @Test
    void verifyReturn_markFailedWhenResponseCodeNotSuccess() {
        VNPayProperties props = new VNPayProperties();
        props.setHashSecret(HASH_SECRET);
        VNPayGateway gw = new VNPayGateway(props, mock(com.example.sales.repository.PaymentTransactionRepository.class), new ObjectMapper());

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", "9900000");
        params.put("vnp_ResponseCode", "24"); // user cancelled
        params.put("vnp_TransactionStatus", "02");
        params.put("vnp_TxnRef", "VNPABC999");
        String sig = PaymentSignatureUtil.hmacSha512Hex(VNPayGateway.buildHashData(params), HASH_SECRET);
        Map<String, String> withHash = new LinkedHashMap<>(params);
        withHash.put("vnp_SecureHash", sig);

        VNPayGateway.VerifyResult result = gw.verifyReturn(withHash);
        assertThat(result.valid()).isTrue();
        assertThat(result.success()).isFalse();
        assertThat(result.responseCode()).isEqualTo("24");
    }
}

