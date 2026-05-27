package com.github.mtprotoproxy.crypto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.IGEBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public class AES256IGE {
    private final IGEBlockCipher cipher;
    private final byte[] key;
    private final byte[] iv;

    public AES256IGE(byte[] key, byte[] iv) {
        if (key.length != 32) throw new IllegalArgumentException("Key must be 32 bytes for AES-256");
        if (iv.length != 32) throw new IllegalArgumentException("IV must be 32 bytes for AES-256-IGE");
        this.key = key.clone();
        this.iv = iv.clone();
        this.cipher = new IGEBlockCipher(new AESEngine());
    }

    public ByteBuf encrypt(ByteBuf plaintext) {
        byte[] input = new byte[plaintext.readableBytes()];
        plaintext.getBytes(plaintext.readerIndex(), input);
        byte[] output = new byte[input.length];
        CipherParameters params = new ParametersWithIV(new KeyParameter(key), iv);
        cipher.init(true, params);
        int processed = 0;
        while (processed < input.length) {
            int len = Math.min(input.length - processed, cipher.getBlockSize());
            if (len == cipher.getBlockSize()) {
                processed += cipher.processBlock(input, processed, output, processed);
            } else {
                // Padding: PKCS#7
                byte[] padded = new byte[cipher.getBlockSize()];
                System.arraycopy(input, processed, padded, 0, len);
                byte padVal = (byte) (cipher.getBlockSize() - len);
                for (int i = len; i < padded.length; i++) padded[i] = padVal;
                cipher.processBlock(padded, 0, padded, 0);
                System.arraycopy(padded, 0, output, processed, padded.length);
                processed += padded.length;
            }
        }
        return Unpooled.wrappedBuffer(output);
    }

    public ByteBuf decrypt(ByteBuf ciphertext) {
        byte[] input = new byte[ciphertext.readableBytes()];
        ciphertext.getBytes(ciphertext.readerIndex(), input);
        byte[] output = new byte[input.length];
        CipherParameters params = new ParametersWithIV(new KeyParameter(key), iv);
        cipher.init(false, params);
        int processed = 0;
        while (processed < input.length) {
            int len = Math.min(input.length - processed, cipher.getBlockSize());
            byte[] block = new byte[cipher.getBlockSize()];
            System.arraycopy(input, processed, block, 0, len);
            if (len < cipher.getBlockSize()) {
                // This shouldn't happen with proper padding
                break;
            }
            cipher.processBlock(block, 0, block, 0);
            System.arraycopy(block, 0, output, processed, cipher.getBlockSize());
            processed += cipher.getBlockSize();
        }
        // Remove PKCS#7 padding
        int padLen = output[output.length - 1] & 0xFF;
        if (padLen > 0 && padLen <= cipher.getBlockSize()) {
            return Unpooled.wrappedBuffer(output, 0, output.length - padLen);
        }
        return Unpooled.wrappedBuffer(output);
    }
}