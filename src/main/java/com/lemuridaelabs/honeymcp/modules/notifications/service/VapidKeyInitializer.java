package com.lemuridaelabs.honeymcp.modules.notifications.service;

import com.lemuridaelabs.honeymcp.modules.notifications.dao.VapidKeyRepository;
import com.lemuridaelabs.honeymcp.modules.notifications.dto.VapidKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.time.Instant;
import java.util.Base64;

@RequiredArgsConstructor
@Component
@Slf4j
public class VapidKeyInitializer {

    private final VapidKeyRepository repository;

    /**
     * Initializes the Vapid push notification keys if they do not already exist
     * in the database. This method is automatically invoked when the application
     * is fully started and ready via the {@code ApplicationReadyEvent}.
     *
     * The method performs the following steps:
     * 1. Checks whether any records exist in the {@code VapidKeyRepository}.
     *    If the repository is not empty, the method exits early.
     * 2. Logs the start of the key generation process.
     * 3. Generates a new KeyPair using the {@code VapidKeyUtil}.
     * 4. Encodes the generated public and private keys using the
     *    {@code VapidEncodingUtil}.
     * 5. Saves the keys in the repository as a {@code VapidKey} record
     *    with a pre-defined ID and a timestamp.
     * 6. Logs the successful creation of the keys.
     *
     * @throws Exception if an error occurs during the key generation process.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() throws Exception {

        if (repository.count() > 0) {
            return;
        }

        log.info("Generating the Vapid push notification keys..");

        var keyPair = VapidKeyUtil.generateKeyPair();

        // Extract raw public key bytes (65 bytes uncompressed format for P-256)
        var ecPublicKey = (ECPublicKey) keyPair.getPublic();
        byte[] rawPublicKey = extractRawPublicKey(ecPublicKey);
        var publicKey = VapidEncodingUtil.encode(rawPublicKey);

        // Extract raw private key bytes (32 bytes scalar for P-256)
        var ecPrivateKey = (ECPrivateKey) keyPair.getPrivate();
        byte[] rawPrivateKey = extractRawPrivateKey(ecPrivateKey);
        var privateKey = VapidEncodingUtil.encode(rawPrivateKey);

        var result = repository.save(new VapidKey(
                1L,
                publicKey,
                privateKey,
                Instant.now()
        ));

        log.info("Generated Vapid push notification keys, result={}", result);

        // Verify the key was actually saved
        var count = repository.count();
        log.info("VAPID key count after save: {}", count);

        if (count == 0) {
            log.error("VAPID key was not persisted to database! Transaction may have failed.");
        } else {
            log.info("VAPID key successfully persisted to database with ID: {}", result.getId());
        }

    }

    /**
     * Extracts the raw public key bytes from an ECPublicKey.
     * For P-256 (secp256r1), this returns 65 bytes in uncompressed format:
     * - 1 byte: 0x04 (uncompressed point indicator)
     * - 32 bytes: X coordinate
     * - 32 bytes: Y coordinate
     *
     * @param ecPublicKey the EC public key to extract bytes from
     * @return raw public key bytes (65 bytes)
     */
    private byte[] extractRawPublicKey(ECPublicKey ecPublicKey) {
        ECPoint point = ecPublicKey.getW();
        BigInteger x = point.getAffineX();
        BigInteger y = point.getAffineY();

        // Convert to byte arrays (32 bytes each for P-256)
        byte[] xBytes = toByteArray(x, 32);
        byte[] yBytes = toByteArray(y, 32);

        // Build uncompressed format: 0x04 || X || Y
        byte[] rawKey = new byte[65];
        rawKey[0] = 0x04; // Uncompressed point format
        System.arraycopy(xBytes, 0, rawKey, 1, 32);
        System.arraycopy(yBytes, 0, rawKey, 33, 32);

        return rawKey;
    }

    /**
     * Extracts the raw private key scalar from an ECPrivateKey.
     * For P-256 (secp256r1), this returns 32 bytes representing the secret scalar.
     *
     * @param ecPrivateKey the EC private key to extract bytes from
     * @return raw private key bytes (32 bytes)
     */
    private byte[] extractRawPrivateKey(ECPrivateKey ecPrivateKey) {
        BigInteger s = ecPrivateKey.getS();
        return toByteArray(s, 32);
    }

    /**
     * Converts a BigInteger to a fixed-size byte array.
     * Pads with leading zeros if necessary.
     *
     * @param value the BigInteger to convert
     * @param length the desired byte array length
     * @return fixed-size byte array
     */
    private byte[] toByteArray(BigInteger value, int length) {
        byte[] bytes = value.toByteArray();

        if (bytes.length == length) {
            return bytes;
        }

        // Handle sign byte and padding
        byte[] result = new byte[length];
        if (bytes.length > length) {
            // Remove leading sign byte
            System.arraycopy(bytes, bytes.length - length, result, 0, length);
        } else {
            // Pad with leading zeros
            System.arraycopy(bytes, 0, result, length - bytes.length, bytes.length);
        }

        return result;
    }

}
