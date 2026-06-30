package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.service.UserService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserInternalController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public UserDto getUser(@PathVariable("userId") Long userId) {
        return userService.getUser(userId);
    }

    @GetMapping("/short/{userId}")
    public UserShortDto getUserShort(@PathVariable("userId") Long userId) {
        return userService.getUserShort(userId);
    }

    @GetMapping
    public List<UserDto> getUsers(@RequestParam List<Long> ids) {
        return userService.getUsers(ids);
    }

    @GetMapping("/short")
    public List<UserShortDto> getUsersShort(@RequestParam List<Long> ids) {
        return userService.getUsersShort(ids);
    }

    @GetMapping("/check/{userId}")
    public void checkUser(@PathVariable("userId") Long userId) {
        userService.checkUser(userId);
    }
}
