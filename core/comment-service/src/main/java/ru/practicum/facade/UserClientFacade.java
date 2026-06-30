package ru.practicum.facade;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.client.UserClient;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.ServiceUnavailableException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserClientFacade {

    private final UserClient userClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
    public UserDto getUser(Long id) {
        return userClient.getUser(id);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUsersFallback")
    public List<UserDto> getUsers(List<Long> ids) {
        return userClient.getUsers(ids);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "checkUserFallback")
    public void checkUser(Long id) {
        userClient.checkUser(id);
    }

    public UserDto getUserFallback(Long id, Throwable t) {
        throw new ServiceUnavailableException("user-service недоступен", t);
    }

    public List<UserDto> getUsersFallback(List<Long> ids, Throwable t) {
        throw new ServiceUnavailableException("user-service недоступен", t);
    }

    public void checkUserFallback(Long id, Throwable t) {
        throw new ServiceUnavailableException("user-service недоступен", t);
    }
}
