package com.piggybank.manager.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import com.piggybank.manager.config.AppProperties;

@Component
public class CryptoUtil {
    private static final int IV_LENGTH = 12;
    private final byte[] encryptionKey;
    private final byte[] tokenKey;
    private final SecureRandom random = new SecureRandom();

    public CryptoUtil(AppProperties properties) throws Exception {
        this.encryptionKey = sha256(properties.getEncryptionKey());
        this.tokenKey = sha256(properties.getTokenSecret());
    }

    public String encrypt(String plain) {
        try {
            // 每次加密使用随机 IV，避免相同明文生成相同密文。
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (Exception ex) {
            throw new IllegalStateException("加密失败", ex);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] all = Base64.getUrlDecoder().decode(encoded);
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[all.length - IV_LENGTH];
            ByteBuffer.wrap(all).get(iv).get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("解密失败", ex);
        }
    }

    public String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(tokenKey, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("签名失败", ex);
        }
    }

    private byte[] sha256(String value) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    }
}
