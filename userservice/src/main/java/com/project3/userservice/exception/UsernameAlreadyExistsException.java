package com.project3.userservice.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String username) {
        super("Username already exists: " + username);
    }
}

