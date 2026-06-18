package ru.practicum.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
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
    public List<UserDto> findUsers(List<Long> ids, Pageable pageable) {
        List<User> found;
        if (ids == null || ids.isEmpty()) {
            found = userRepository.findAll(pageable).getContent();
        } else {
            found = userRepository.findByIdIn(ids, pageable).getContent();
        }
        return userMapper.toUserDtoList(found);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {

        if (!userRepository.existsById(id)) {
            String message = String.format("Пользователь с ID=%d не найден", id);
            log.warn(message);
            throw new NotFoundException(message);
        }

        userRepository.deleteById(id);
        log.info("Пользователь с ID {} успешно удалён", id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        log.info("Пользователь с ID {} найден", id);
        return userMapper.toUserDto(user);
    }
}
