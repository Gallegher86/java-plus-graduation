package ru.practicum.user.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto create(@Valid NewUserRequest newUserRequest) {
        String email = newUserRequest.getEmail();
        log.info("Создание нового пользователя с email: {}", email);

        // Проверка на существование пользователя с таким email
        if (userRepository.existsByEmail(email)) {
            log.warn("Пользователь с email '{}' уже существует", email);
            throw new ConflictException("Пользователь с указанным email уже зарегистрирован");
        }

        User newUser = userMapper.toUser(newUserRequest);
        User savedUser = userRepository.save(newUser);
        log.debug("Пользователь успешно создан с ID: {}", savedUser.getId());
        return userMapper.toUserDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> findUsers(Long[] ids, Pageable pageable) {
        List<User> found;
        if (ids == null) {
            found = userRepository.findAll(pageable).getContent();
        } else {
            found = userRepository.findByIdInWithPagination(Arrays.asList(ids), pageable).getContent();
        }
        return userMapper.toUserDtoList(found);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        long deletedCount = userRepository.deleteByIdCustom(id);

        if (deletedCount == 0) {
            String message = String.format("Пользователь с ID=%d не найден для удаления", id);
            log.warn(message);
            throw new NotFoundException(message);
        }

        log.info("Пользователь с ID {} успешно удалён", id);
    }
}
