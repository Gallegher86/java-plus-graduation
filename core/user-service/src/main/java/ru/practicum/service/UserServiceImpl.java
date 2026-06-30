package ru.practicum.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        log.debug("Запрос на получение пользователя с ID {}", id);
        User user = findUserOrThrow(id);
        log.debug("Пользователь с ID {} найден", id);
        return userMapper.toUserDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserShortDto getUserShort(Long id) {
        log.debug("Запрос на получение пользователя с ID {} в формате UserShortDto", id);
        User user = findUserOrThrow(id);
        log.debug("Пользователь с ID {} найден, выдача в формате UserShortDto", id);
        return userMapper.toUserShortDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsers(List<Long> ids) {
        log.debug("Запрос на получение пользователей с IDs {}", ids);
        List<User> users = userRepository.findAllById(ids);
        validateUserIds(ids, users);
        log.debug("Пользователи с IDs {} найдены", ids);
        return users.stream()
                .map(userMapper::toUserDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserShortDto> getUsersShort(List<Long> ids) {
        log.debug("Запрос на получение пользователей с IDs {} в формате UserShortDto", ids);
        List<User> users = userRepository.findAllById(ids);
        validateUserIds(ids, users);
        log.debug("Пользователи с IDs {} найдены, выдача в формате UserShortDto", ids);
        return users.stream()
                .map(userMapper::toUserShortDto)
                .toList();
    }

    @Override
    public void checkUser(Long id) {
        log.debug("Запрос на проверку существования пользователя с ID {}", id);

        if (!userRepository.existsById(id)) {
            throw new NotFoundException("Пользователь c ID " + id + " не найден.");
        }

        log.info("Пользователь с ID {} существует", id);
    }

    private void validateUserIds(List<Long> requestedIds, List<User> users) {
        Set<Long> foundIds = users.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        List<Long> missingIds = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new NotFoundException("Пользователи не найдены: " + missingIds);
        }
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + id + " не найден"));
    }
}
