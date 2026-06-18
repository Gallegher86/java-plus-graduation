package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.client.config.FeignConfig;
import ru.practicum.dto.user.UserDto;

@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserClient {

    @GetMapping("/internal/users/{userId}")
    UserDto getUser(@PathVariable("userId") Long userId);
}
