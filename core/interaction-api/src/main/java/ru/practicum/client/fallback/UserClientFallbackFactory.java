package ru.practicum.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.client.UserClient;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ServiceUnavailableException;

import java.util.List;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {

            @Override
            public UserDto getUser(Long userId) {
                throw serviceUnavailable(cause);
            }

            @Override
            public UserShortDto getUserShort(Long userId) {
                throw serviceUnavailable(cause);
            }

            @Override
            public List<UserDto> getUsers(List<Long> ids) {
                throw serviceUnavailable(cause);
            }

            @Override
            public List<UserShortDto> getUsersShort(List<Long> ids) {
                throw serviceUnavailable(cause);
            }

            @Override
            public void checkUser(Long userId) {
                throw serviceUnavailable(cause);
            }

            private ServiceUnavailableException serviceUnavailable(Throwable cause) {
                ServiceUnavailableException ex =
                        new ServiceUnavailableException("user-service недоступен", cause);

                log.error("user-service недоступен", cause);
                return ex;
            }
        };
    }
}
