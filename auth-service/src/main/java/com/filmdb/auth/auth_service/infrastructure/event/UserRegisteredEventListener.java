package com.filmdb.auth.auth_service.infrastructure.event;

import com.filmdb.auth.auth_service.domain.event.UserRegisteredEvent;
import com.filmdb.auth.auth_service.domain.model.valueobject.EmailMessage;
import com.filmdb.auth.auth_service.domain.services.MailProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class UserRegisteredEventListener {

    private final MailProvider mailProvider;

    @Async
    @EventListener
    public void handle(UserRegisteredEvent event) {
        log.info("Handling UserRegisteredEvent for user {}", event.getUserId().value());
        EmailMessage confirmationMail = EmailMessage.of(
                event.getUserEmail(),
                "User registration confirmation.",
                "Your user has been registered successfully.");
        mailProvider.sendMail(confirmationMail);
    }
}
