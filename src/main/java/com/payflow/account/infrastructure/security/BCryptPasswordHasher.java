package com.payflow.account.infrastructure.security;

import com.payflow.account.application.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder;

    public BCryptPasswordHasher() {
        this.encoder = new BCryptPasswordEncoder();
    }

    @Override
    public String hash(String rawPassword) {
        Objects.requireNonNull(
                rawPassword,
                "raw password must not be null"
        );

        return encoder.encode(rawPassword);
    }
}