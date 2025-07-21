// File: src/main/java/com/example/sales/util/SignatureUtil.java
package com.example.sales.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class SignatureUtil {

    public static boolean isValidHmac(String payload, String signature, String secret) {
        try {
            Mac sha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256.init(keySpec);
            byte[] hash = sha256.doFinal(payload.getBytes());
            String expected = Base64.getEncoder().encodeToString(hash);
            return expected.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
