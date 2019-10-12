package de.derrop.labymod.addons.server.util;
/*
 * Created by derrop on 11.10.2019
 */

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

public class Utility {

    private Utility() {
        throw new UnsupportedOperationException();
    }

    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();

    public static final Random RANDOM = new Random();

    public static String hashString(String input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(input.getBytes(StandardCharsets.UTF_8));

            return new String(Base64.getMimeEncoder().encode(messageDigest.digest()), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return input;
    }

    public static UUID convertBytesToUUID(byte[] bytes) {
        if (bytes != null) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return null;
    }

    public static byte[] convertUUIDToBytes(UUID uuid) {
        return uuid != null ?
                ByteBuffer.wrap(new byte[16]).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array() :
                null;
    }

    public static String generateRandomString(int length) {
        char[] result = new char[length];
        for (int i = 0; i < length; i++) {
            result[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return String.valueOf(result);
    }

    public static String generateRandomString(int minLength, int maxLength) {
        return generateRandomString(RANDOM.nextInt(maxLength - minLength) + minLength);
    }

}
