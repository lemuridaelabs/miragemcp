package com.lemuridaelabs.honeymcp.modules.notifications.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;

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
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");

        keyPairGenerator.initialize(ecSpec);
        return keyPairGenerator.generateKeyPair();
    }

}
