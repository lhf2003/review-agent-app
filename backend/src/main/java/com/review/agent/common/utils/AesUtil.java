package com.review.agent.common.utils;

import com.review.agent.common.constant.UserConstant;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AesUtil {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTE = 12;
    private static final int TAG_LENGTH_BIT = 128;

    public static String decrypt(String encryptedStr) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedStr);
            
            // Extract IV
            if (decoded.length < IV_LENGTH_BYTE) throw new IllegalArgumentException("Invalid encrypted data");
            byte[] iv = new byte[IV_LENGTH_BYTE];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH_BYTE);
            
            // Extract Ciphertext
            byte[] ciphertext = new byte[decoded.length - IV_LENGTH_BYTE];
            System.arraycopy(decoded, IV_LENGTH_BYTE, ciphertext, 0, ciphertext.length);
            
            SecretKeySpec keySpec = new SecretKeySpec(UserConstant.AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            
            byte[] original = cipher.doFinal(ciphertext);
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
