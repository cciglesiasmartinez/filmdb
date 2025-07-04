package com.filmdb.auth.auth_service.application.exception;

public class UserIsNotExternalException extends RuntimeException {

    public UserIsNotExternalException() {
        super("User is not an externally authenticated user.");
    }

}
