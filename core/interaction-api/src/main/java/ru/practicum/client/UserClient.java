package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.client.config.FeignConfig;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;

import java.util.List;

@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserClient {

    @GetMapping("/internal/users/{userId}")
    UserDto getUser(@PathVariable("userId") Long userId);

    @GetMapping("/internal/users/short/{userId}")
    UserShortDto getUserShort(@PathVariable("userId") Long userId);

    @GetMapping("/internal/users")
    List<UserDto> getUsers(@RequestParam List<Long> ids);

    @GetMapping("/internal/users/short")
    List<UserShortDto> getUsersShort(@RequestParam List<Long> ids);

    @GetMapping("/internal/users/check/{userId}")
    void checkUser(@PathVariable("userId") Long userId);
}
