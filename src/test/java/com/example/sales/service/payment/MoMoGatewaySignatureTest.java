package com.example.sales.service.payment;

import com.example.sales.config.MomoProperties;
import com.example.sales.util.PaymentSignatureUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit test cho logic build signature và verifyIpn của MoMo — không gọi network.
 */
class MoMoGatewaySignatureTest {

    private static final String ACCESS_KEY = "F8BBA842ECF85";
    private static final String SECRET_KEY = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    private static final String PARTNER_CODE = "MOMO";

    @Test
    void initSignaturePayload_followsDocumentedKeyOrder() {
        String payload = MoMoGateway.buildInitSignaturePayload(
                ACCESS_KEY,
                99_000L,
                "",
                "http://localhost:8080/api/webhook/momo",
                "ORDER123",
                "Thanh toan",
                PARTNER_CODE,
                "http://localhost:5173/billing",
                "REQ123",
                "captureWallet"
        );
        assertThat(payload).isEqualTo(
                "accessKey=" + ACCESS_KEY
                        + "&amount=99000"
                        + "&extraData="
                        + "&ipnUrl=http://localhost:8080/api/webhook/momo"
                        + "&orderId=ORDER123"
                        + "&orderInfo=Thanh toan"
                        + "&partnerCode=" + PARTNER_CODE
                        + "&redirectUrl=http://localhost:5173/billing"
                        + "&requestId=REQ123"
                        + "&requestType=captureWallet");
    }

    @Test
    void hmacSha256Hex_isDeterministicAndHex() {
        String sig = PaymentSignatureUtil.hmacSha256Hex("abc=1&def=2", SECRET_KEY);
        assertThat(sig).hasSize(64).matches("[0-9a-f]+");
        assertThat(sig).isEqualTo(PaymentSignatureUtil.hmacSha256Hex("abc=1&def=2", SECRET_KEY));
    }

    @Test
    void verifyIpn_acceptsValidSignature() {
        MomoProperties props = new MomoProperties();
        props.setAccessKey(ACCESS_KEY);
        props.setSecretKey(SECRET_KEY);
        props.setPartnerCode(PARTNER_CODE);
        MoMoGateway gw = new MoMoGateway(props,
                mock(com.example.sales.repository.PaymentTransactionRepository.class),
                new ObjectMapper());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", PARTNER_CODE);
        payload.put("orderId", "ORDER123");
        payload.put("requestId", "REQ123");
        payload.put("amount", "99000");
        payload.put("orderInfo", "Thanh toan");
        payload.put("orderType", "momo_wallet");
        payload.put("transId", "999888777");
        payload.put("resultCode", "0");
        payload.put("message", "Successful");
        payload.put("payType", "qr");
        payload.put("responseTime", "1700000000000");
        payload.put("extraData", "");

        String raw = MoMoGateway.buildIpnSignaturePayload(
                ACCESS_KEY,
                "99000",
                "",
                "Successful",
                "ORDER123",
                "Thanh toan",
                "momo_wallet",
                PARTNER_CODE,
                "qr",
                "REQ123",
                "1700000000000",
                "0",
                "999888777"
        );
        String sig = PaymentSignatureUtil.hmacSha256Hex(raw, SECRET_KEY);
        payload.put("signature", sig);

        MoMoGateway.VerifyResult result = gw.verifyIpn(payload);

        assertThat(result.valid()).isTrue();
        assertThat(result.success()).isTrue();
        assertThat(result.orderId()).isEqualTo("ORDER123");
        assertThat(result.providerTransNo()).isEqualTo("999888777");
        assertThat(result.amountVnd()).isEqualTo(99_000L);
    }

    @Test
    void verifyIpn_rejectsWrongSignature() {
        MomoProperties props = new MomoProperties();
        props.setAccessKey(ACCESS_KEY);
        props.setSecretKey(SECRET_KEY);
        props.setPartnerCode(PARTNER_CODE);
        MoMoGateway gw = new MoMoGateway(props,
                mock(com.example.sales.repository.PaymentTransactionRepository.class),
                new ObjectMapper());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", "ORDER123");
        payload.put("resultCode", "0");
        payload.put("amount", "99000");
        payload.put("signature", "deadbeef");

        assertThat(gw.verifyIpn(payload).valid()).isFalse();
    }

    @Test
    void verifyIpn_rejectsEmptyPayload() {
        MomoProperties props = new MomoProperties();
        props.setSecretKey(SECRET_KEY);
        MoMoGateway gw = new MoMoGateway(props,
                mock(com.example.sales.repository.PaymentTransactionRepository.class),
                new ObjectMapper());
        assertThat(gw.verifyIpn(Map.of()).valid()).isFalse();
    }
}
