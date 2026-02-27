package com.lemuridaelabs.miragemcp.modules.notifications.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Base64;

/**
 * Utility class for encoding VAPID keys in URL-safe Base64 format.
 *
 * <p>The Web Push specification requires keys to be encoded in base64url format
 * without padding. This utility provides the encoding functionality.</p>
 *
 * @see VapidKeyInitializer
 * @since 1.0
 */
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
