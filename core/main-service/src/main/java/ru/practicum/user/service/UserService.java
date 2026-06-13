package ru.practicum.user.service;


import org.springframework.data.domain.Pageable;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto create(NewUserRequest newUserRequest);

    List<UserDto> findUsers(Long[] ids, Pageable pageable);

    void deleteUser(Long id);
}