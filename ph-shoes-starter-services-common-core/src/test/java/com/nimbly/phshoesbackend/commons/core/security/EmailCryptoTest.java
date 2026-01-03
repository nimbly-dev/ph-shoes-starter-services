package com.nimbly.phshoesbackend.commons.core.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nimbly.phshoesbackend.commons.core.config.EmailSecurityProperties;

class EmailCryptoTest {

    @Test
    void normalize_trimsAndLowerCases() {
        EmailCrypto crypto = new EmailCrypto(buildProperties());

        assertEquals("test+label@gmail.com", crypto.normalize(" Test+Label@Gmail.Com "));
    }

    @Test
    void hashCandidates_includesVariants() {
        EmailCrypto crypto = new EmailCrypto(buildProperties());

        List<String> candidates = crypto.hashCandidates("user.name+tag@gmail.com");

        assertTrue(candidates.size() > 1);
    }

    @Test
    void encrypt_roundTrips() {
        EmailCrypto crypto = new EmailCrypto(buildProperties());

        String encrypted = crypto.encrypt("person@example.com");
        String decrypted = crypto.decrypt(encrypted);

        assertEquals("person@example.com", decrypted);
    }

    @Test
    void matchesLegacyHash_withSharedKey() {
        EmailSecurityProperties properties = buildProperties();
        properties.setLegacyPeppersB64(List.of(properties.getHmacPepperB64()));
        EmailCrypto crypto = new EmailCrypto(properties);

        String hash = crypto.hash("legacy@example.com");

        assertTrue(crypto.matchesLegacyHash(hash, "legacy@example.com"));
        assertFalse(crypto.matchesLegacyHash(hash, "other@example.com"));
    }

    private static EmailSecurityProperties buildProperties() {
        EmailSecurityProperties properties = new EmailSecurityProperties();
        properties.setHmacPepperB64(Base64.getEncoder().encodeToString(fixedBytes(32, (byte) 0x1f)));
        properties.setAesKeyB64(Base64.getEncoder().encodeToString(fixedBytes(32, (byte) 0x2a)));
        assertNotNull(properties.getHmacPepperB64());
        assertNotNull(properties.getAesKeyB64());
        return properties;
    }

    private static byte[] fixedBytes(int length, byte value) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = value;
        }
        return bytes;
    }
}
