package ru.practicum.mapper;

import org.mapstruct.Mapper;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.model.User;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(NewUserRequest dto);

    UserDto toUserDto(User user);

    List<UserDto> toUserDtoList(List<User> users);

    UserShortDto toUserShortDto(User user);

    List<UserShortDto> toUserShortDtoList(List<User> users);
}
