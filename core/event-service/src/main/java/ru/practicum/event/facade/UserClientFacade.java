package ru.practicum.event.facade;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.client.UserClient;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ServiceUnavailableException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserClientFacade {

    private final UserClient userClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserShortFallback")
    public UserShortDto getUserShort(Long id) {
        return userClient.getUserShort(id);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUsersShortFallback")
    public List<UserShortDto> getUsersShort(List<Long> ids) {
        return userClient.getUsersShort(ids);
    }

    public UserShortDto getUserShortFallback(Long id, Throwable t) {
        throw new ServiceUnavailableException("user-service недоступен", t);
    }

    public List<UserShortDto> getUsersShortFallback(List<Long> ids, Throwable t) {
        throw new ServiceUnavailableException("user-service недоступен", t);
    }
}
