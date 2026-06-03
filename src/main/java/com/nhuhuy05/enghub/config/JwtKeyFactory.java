package com.nhuhuy05.enghub.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class JwtKeyFactory {
    private JwtKeyFactory() {
    }

    public static byte[] hs512Key(String signerKey) {
        if (signerKey == null || signerKey.isBlank()) {
            throw new IllegalStateException("JWT signer key must be configured");
        }

        try {
            return MessageDigest.getInstance("SHA-512")
                    .digest(signerKey.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 algorithm is not available", e);
        }
    }
}
