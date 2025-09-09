package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;

class User {
    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("preferences")
    private Preferences preferences;

    public User() {}

    public User(String username, String email, Preferences preferences) {
        this.username = username;
        this.email = email;
        this.preferences = preferences;
    }

    @Override
    public String toString() {
        return String.format("com.example.User{username='%s', email='%s', preferences=%s}",
                username, email, preferences);
    }
}