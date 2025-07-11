package com.filmdb.auth.auth_service.application.usecases;

import com.filmdb.auth.auth_service.adapter.in.web.dto.responses.LoginResponse;
import com.filmdb.auth.auth_service.application.commands.OAuthGoogleRegisterUserCommand;
import com.filmdb.auth.auth_service.application.exception.EmailAlreadyExistsException;
import com.filmdb.auth.auth_service.application.services.RefreshTokenService;
import com.filmdb.auth.auth_service.domain.model.RefreshToken;
import com.filmdb.auth.auth_service.domain.model.User;
import com.filmdb.auth.auth_service.domain.model.UserLogin;
import com.filmdb.auth.auth_service.domain.model.valueobject.Email;
import com.filmdb.auth.auth_service.domain.model.valueobject.ProviderKey;
import com.filmdb.auth.auth_service.domain.model.valueobject.ProviderName;
import com.filmdb.auth.auth_service.domain.repository.UserLoginRepository;
import com.filmdb.auth.auth_service.domain.repository.UserRepository;
import com.filmdb.auth.auth_service.domain.services.AccessTokenProvider;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class OAuthGoogleRegisterUserUseCase {

    private final UserRepository userRepository;
    private final UserLoginRepository userLoginRepository;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public LoginResponse execute(OAuthGoogleRegisterUserCommand command) {
        ProviderKey providerKey = ProviderKey.of(command.googleId());
        ProviderName providerName = ProviderName.GOOGLE;
        Email googleEmail = Email.of(command.googleEmail());
        if (userRepository.existsByEmail(googleEmail)) {
            throw new EmailAlreadyExistsException();
        }
        User user = User.createExternalUser(googleEmail, providerKey, providerName);
        userRepository.save(user);
        UserLogin userLogin = UserLogin.create(user.id(), providerKey, providerName);
        userLoginRepository.save(userLogin);
        // Token logic
        String token = accessTokenProvider.generateToken(user.id().value());
        long expiresIn = accessTokenProvider.getTokenExpirationInSeconds();
        RefreshToken refreshToken = refreshTokenService.generate(user.id(), command.ip(), command.userAgent());
        log.info("User registered via Google OAuth successfully");
        return new LoginResponse(token, refreshToken.token().value() , expiresIn, user.username().value());
    }

}
