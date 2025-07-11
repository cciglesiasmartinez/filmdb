package com.filmdb.auth.auth_service.infrastructure.security;

import com.filmdb.auth.auth_service.domain.model.valueobject.VerificationCodeString;
import com.filmdb.auth.auth_service.domain.services.VerificationCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class Base64VerificationCodeGenerator implements VerificationCodeGenerator {

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public VerificationCodeString generate() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return VerificationCodeString.of(code);
    }
}
