package com.filmdb.auth.auth_service.application.commands;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class OAuthGoogleLoginUserCommand {

    private final String googleId;
    private final String googleEmail;
    private final String ip;
    private final String userAgent;

    public String googleId() {
        return this.googleId;
    }

    public String googleEmail() {
        return this.googleEmail;
    }

    public String ip() {
        return this.ip;
    }

    public String userAgent() {
        return this.userAgent;
    }

}
