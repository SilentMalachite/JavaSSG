package com.javassg.model;

public record Author(String name, String email) {
    public Author {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("著者名は空にできません");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("メールアドレスは空にできません");
        }
    }
}