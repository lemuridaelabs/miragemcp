package com.lemuridaelabs.honeymcp.modules.notifications.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Base64;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VapidEncodingUtil {

    /**
     * Encodes the given byte array into a URL-safe Base64 encoded string without padding.
     *
     * @param bytes the byte array to be encoded
     * @return a URL-safe Base64 encoded string representation of the input byte array
     */
    public static String encode(byte[] bytes) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

}
