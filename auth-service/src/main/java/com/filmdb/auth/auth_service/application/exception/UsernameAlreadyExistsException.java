package com.filmdb.auth.auth_service.application.exception;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException() {
        super("Username already exists.");
    }

}
