package ru.practicum.facade;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.client.UserClient;
import ru.practicum.exception.ServiceUnavailableException;

@Component
@RequiredArgsConstructor
public class UserClientFacade {

    private final UserClient userClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "checkUserFallback")
    public void checkUser(Long id) {
        userClient.checkUser(id);
    }

    public void checkUserFallback(Long id, Throwable t) {
        throw new ServiceUnavailableException("user-service недоступен", t);
    }
}
