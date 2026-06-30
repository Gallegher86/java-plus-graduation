package ru.practicum.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;

import java.util.List;

public interface UserService {
    UserDto create(NewUserRequest newUserRequest);

    List<UserDto> findUsers(List<Long> ids, Pageable pageable);

    void deleteUser(Long id);

    UserDto getUser(Long id);

    UserShortDto getUserShort(Long id);

    List<UserDto> getUsers(List<Long> ids);

    List<UserShortDto> getUsersShort(List<Long> ids);

    void checkUser(Long id);
}
