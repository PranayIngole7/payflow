package com.payflow.account.application;

public interface PasswordHasher {

    String hash(String rawPassword);
}