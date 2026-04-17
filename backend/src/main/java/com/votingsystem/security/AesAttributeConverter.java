package com.votingsystem.security;

import com.votingsystem.config.SecurityProperties;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * =============================================================================
 * FILE: AesAttributeConverter.java
 * PACKAGE: com.votingsystem.security
 * =============================================================================
 *
 * <h2>IAS101 "SECURED SUPER SYSTEM" — RUBRIC MAPPING</h2>
 * <ul>
 * <li><b>[ENCRYPTION AT REST — AES-256]:</b> Intercepts JPA read/write operations
 * to transparently encrypt PII fields before writing to PostgreSQL, and decrypt
 * upon reading. Uses AES-256 in GCM mode for authenticity and confidentiality.</li>
 * </ul>
 */
@Converter
@Component
public class AesAttributeConverter implements AttributeConverter<String, String> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AesAttributeConverter.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecurityProperties securityProperties;

    public AesAttributeConverter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    // Use a secure random instance for IV generation
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (!StringUtils.hasText(attribute)) {
            return attribute;
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKeySpec keySpec = new SecretKeySpec(securityProperties.aesSecretKey().getBytes(StandardCharsets.UTF_8), "AES");
            
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
            byte[] ciphertext = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            
            // Prepend IV to the ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);
            
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Failed to encrypt database column attribute.", e);
            throw new RuntimeException("Encryption error during persistence.", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasText(dbData)) {
            return dbData;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);
            
            // Extract the IV directly from the decoded payload
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKeySpec keySpec = new SecretKeySpec(securityProperties.aesSecretKey().getBytes(StandardCharsets.UTF_8), "AES");
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
            byte[] plaintext = cipher.doFinal(ciphertext);
            
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt database column attribute.", e);
            throw new RuntimeException("Decryption error during persistence read.", e);
        }
    }
}
