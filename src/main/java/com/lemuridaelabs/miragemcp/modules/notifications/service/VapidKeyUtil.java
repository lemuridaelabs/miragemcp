package com.lemuridaelabs.miragemcp.modules.notifications.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;

/**
 * Utility class for generating VAPID key pairs.
 *
 * <p>Provides cryptographic key pair generation using the Elliptic Curve (EC) algorithm
 * with the secp256r1 (P-256) curve, as required by the Web Push specification.</p>
 *
 * @see VapidKeyInitializer
 * @since 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VapidKeyUtil {

    /**
     * Generates a cryptographic KeyPair using the Elliptic Curve (EC) algorithm
     * and the "secp256r1" curve specification.
     *
     * @return a KeyPair containing a public and a private key, based on the specified EC curve
     * @throws Exception if an error occurs during the key pair generation process
     */
    public static KeyPair generateKeyPair() throws Exception {
        var keyPairGenerator = KeyPairGenerator.getInstance("EC");
        var ecSpec = new ECGenParameterSpec("secp256r1");

        keyPairGenerator.initialize(ecSpec);
        return keyPairGenerator.generateKeyPair();
    }

}
