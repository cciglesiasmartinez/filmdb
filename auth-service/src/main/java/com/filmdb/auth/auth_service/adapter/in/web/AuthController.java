package com.filmdb.auth.auth_service.adapter.in.web;

import com.filmdb.auth.auth_service.adapter.in.web.dto.requests.*;
import com.filmdb.auth.auth_service.adapter.in.web.dto.responses.*;
import com.filmdb.auth.auth_service.application.context.RequestContext;
import com.filmdb.auth.auth_service.application.usecases.AuthUseCase;
import com.filmdb.auth.auth_service.domain.model.User;
import com.filmdb.auth.auth_service.application.exception.InvalidCredentialsException;
import com.filmdb.auth.auth_service.infrastructure.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;

@AllArgsConstructor
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints related to user authentication, registration, and account " +
        "management.")
public class AuthController {

    private final AuthUseCase authUseCase;

    /**
     * Helper method that obtains a model {@link User} instance from the {@link Authentication} instance in a request.
     *
     * @param authentication {@link Authentication} instance containing the authentication data from the request.
     * @throws InvalidCredentialsException if credentials are nonexistent or malformed.
     * @return a {@link User} instance.
     */
    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails customUserDetails)) {
            throw new InvalidCredentialsException("Unauthorized");
        }
        return customUserDetails.getUser();
    }

    @Operation(
            summary = "Register a new user.",
            description = "Initiates the registration process by sending a verification email. User will not be " +
                    "persisted until verification is complete."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Verification email sent successfully.")
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authUseCase.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(
            summary="Verify a registration code.",
            description="Verifies an email account thus completing the registration process and persisting the user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "User registration completed.")
    })
    @GetMapping("/register/verify")
    public ResponseEntity<?> verifyRegistration(@RequestParam("code") String code) {
        UserResponse response = authUseCase.verifyRegistration(code);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @Operation(
            summary = "Authenticate user.",
            description = "Validates user credentials and returns access and refresh tokens."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Authentication successful.")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        RequestContext context = new RequestContext(ip, userAgent);
        LoginResponse response = authUseCase.login(request, context);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @Operation(
            summary = "Get an access token and a new refresh token.",
            description = "Provides a signed access token and a new refresh token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Access token and new refresh token issued.")
    })
    @PostMapping("/refresh")
    public ResponseEntity<RefreshAccessTokenResponse> refresh(@Valid @RequestBody RefreshAccessTokenRequest request) {
        RefreshAccessTokenResponse response = authUseCase.refreshAccessToken(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Retrieves user information (self).",
            description = "Retrieves all available user (self) information excluding sensitive data."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User information successfully retrieved.")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        UserResponse response = UserResponse.fromDomainUser(user);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Change user (self) password,",
            description = "Changes user (self) password if provided current password matches stored one."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User (self) password changed successfully.")
    })
    @PutMapping("/me/password")
    public ResponseEntity<ChangePasswordResponse> changeUserPassword(@Valid @RequestBody ChangePasswordRequest request,
                                                                     Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        authUseCase.changePassword(user, request);
        ChangePasswordResponse response = new ChangePasswordResponse("Password changed", LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Change user (self) username.",
            description = "Changes user (self) username if provided current password matches stored one."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User (self) username changed successfully.")
    })
    @PutMapping("/me/username")
    public ResponseEntity<ChangeUsernameResponse> changeUserUsername(@Valid @RequestBody ChangeUsernameRequest request,
                                                                     Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        ChangeUsernameResponse response = authUseCase.changeUsername(user, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Change user (self) email.",
            description = "Changes user (self) email if provided current password matches stored one."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User (self) email changed successfully.")
    })
    @PutMapping("/me/email")
    public ResponseEntity<ChangeEmailResponse> changeUserEmail(@Valid @RequestBody ChangeEmailRequest request,
                                                               Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        ChangeEmailResponse response = authUseCase.changeEmail(user, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Delete user (self).",
            description = "Deletes user (self) account if provided current password matches stored one."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User (self) account deleted successfully.")
    })
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteUser(@Valid @RequestBody DeleteUserRequest request,
                                        Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        authUseCase.deleteUser(user, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Redirect user to Google OAuth2 login.",
            description = "Redirects user to Google OAuth2 sign in."
    )
    @GetMapping("/oauth/google")
    public RedirectView redirectToGoogle(@Value("${google.oauth.client-id}") String clientId,
                                         @Value("${google.oauth.redirect-uri}") String redirectUri) {
        String uri = "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=openid%20email%20profile" +
                "&access_type=offline" +
                "&prompt=consent";
        return new RedirectView(uri);
    }

    @Operation(
            summary = "Callback route for Google OAuth2.",
            description = "Intended for internal application use. Handles Google OAuth data request for the user. " +
                    "If user is not registered yet, it registers them in our database. If yes, proceeds to login " +
                    "the user in our system. "
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully logged in via Google OAuth2.")
    })
    @GetMapping("/oauth/google/callback")
    public ResponseEntity<LoginResponse> handleGoogleCallback(
            @RequestParam("code") String code,
            HttpServletRequest httpRequest,
            @Value("${google.oauth.client-id}") String clientId,
            @Value("${google.oauth.redirect-uri}") String redirectUri,
            @Value("${google.oauth.client-secret}") String clientSecret) {
        WebClient webClient = WebClient.create();
        OAuthGoogleRequest googleResponse = webClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("code", code)
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("redirect_uri", redirectUri)
                        .with("grant_type", "authorization_code"))
                .retrieve()
                .bodyToMono(OAuthGoogleRequest.class)
                .block();
        String ip = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        RequestContext context = new RequestContext(ip, userAgent);
        LoginResponse response = authUseCase.OAuthGoogleFlow(googleResponse, context);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @Operation(
            summary = "Changes user (self) username (OAuth2).",
            description = "Changes user (self) username. This is the right endpoint if user is authenticated via" +
                    "OAuth2."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully logged in via Google OAuth2.")
    })
    @PutMapping("/oauth/me/username")
    public ResponseEntity<ChangeUsernameResponse> changeExternalUserUsername(
            @Valid @RequestBody ChangeExternalUserUsernameRequest request,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        ChangeUsernameResponse response = authUseCase.changeExternalUserUsername(user, request);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @Operation(
            summary = "Delete OAuth2 user (self).",
            description = "Deletes OAuth2 user (self). This is the right endpoint if user is authenticated via" +
                    "OAuth2."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "OAuth2 user (self) deleted successfully.")
    })
    @DeleteMapping("/oauth/me")
    public ResponseEntity<?> deleteExternalUser(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        authUseCase.deleteExternalUser(user);
        return ResponseEntity.noContent().build();
    }

}
