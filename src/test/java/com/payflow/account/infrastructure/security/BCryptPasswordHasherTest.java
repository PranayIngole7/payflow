package com.payflow.account.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher passwordHasher =
            new BCryptPasswordHasher();

    @Test
    void shouldHashPassword() {
        String rawPassword = "StrongPassword123!";

        String hash = passwordHasher.hash(rawPassword);

        assertNotNull(hash);
        assertNotEquals(rawPassword, hash);
    }

    @Test
    void shouldMatchCorrectPassword() {
        String rawPassword = "StrongPassword123!";
        String hash = passwordHasher.hash(rawPassword);

        assertTrue(
                passwordHasher.matches(rawPassword, hash)
        );
    }

    @Test
    void shouldRejectIncorrectPassword() {
        String hash = passwordHasher.hash("StrongPassword123!");

        assertFalse(
                passwordHasher.matches(
                        "WrongPassword123!",
                        hash
                )
        );
    }

    @Test
    void shouldRejectNullRawPasswordWhenHashing() {
        assertThrows(
                NullPointerException.class,
                () -> passwordHasher.hash(null)
        );
    }

    @Test
    void shouldRejectNullRawPasswordWhenMatching() {
        String hash = passwordHasher.hash("StrongPassword123!");

        assertThrows(
                NullPointerException.class,
                () -> passwordHasher.matches(null, hash)
        );
    }

    @Test
    void shouldRejectNullPasswordHashWhenMatching() {
        assertThrows(
                NullPointerException.class,
                () -> passwordHasher.matches(
                        "StrongPassword123!",
                        null
                )
        );
    }
}
