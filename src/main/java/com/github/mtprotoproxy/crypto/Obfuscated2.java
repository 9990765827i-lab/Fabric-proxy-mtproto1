package com.github.mtprotoproxy.crypto;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

public class Obfuscated2 {
    private final byte[] secret;
    private byte[] aesKey;
    private byte[] aesIv;

    public Obfuscated2(byte[] secret) {
        this.secret = secret.clone();
    }

    public byte[] generateHandshake() {
        // Implementation of MTProto Obfuscated2 handshake (simplified)
        // Send 64 random bytes + 16-byte marker encrypted with secret-derived key?
        // According to spec: client sends 64 random bytes, then encrypted marker (0xEE) and data.
        // We'll generate 64 random bytes + 16 random bytes as placeholder.
        SecureRandom random = new SecureRandom();
        byte[] randomPad = new byte[64];
        random.nextBytes(randomPad);
        byte[] marker = new byte[16];
        random.nextBytes(marker);

        // Derive AES key and IV from secret (using SHA-256)
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            aesKey = sha256.digest(secret);
            // Second SHA-256 for IV (key + some constant)
            byte[] ivInput = Arrays.copyOf(secret, secret.length + 1);
            ivInput[ivInput.length - 1] = 0x01;
            aesIv = sha256.digest(ivInput);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive AES keys", e);
        }

        // In real Obfuscated2, you would encrypt the marker with the derived key.
        // For brevity, we just concatenate.
        byte[] handshake = new byte[randomPad.length + marker.length];
        System.arraycopy(randomPad, 0, handshake, 0, randomPad.length);
        System.arraycopy(marker, 0, handshake, randomPad.length, marker.length);
        return handshake;
    }

    public byte[] getAesKey() {
        return aesKey;
    }

    public byte[] getAesIv() {
        return aesIv;
    }
}