package com.bank.aiassistant.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class FileHashUtils {

    private static final int BUFFER_SIZE = 8192;

    private FileHashUtils() {
    }

    public static String sha256(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return sha256(inputStream);
        }
    }

    public static String sha256(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (digestInputStream.read(buffer) != -1) {
                    // DigestInputStream updates the digest as bytes are read.
                }
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new IllegalStateException("Failed to calculate SHA-256 hash", ex);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
