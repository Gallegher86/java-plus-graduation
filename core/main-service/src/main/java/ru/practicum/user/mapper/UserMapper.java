package ru.practicum.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserShortDto;
import ru.practicum.user.model.User;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "email", target = "email")
    @Mapping(source = "name", target = "name")
    User toUser(NewUserRequest dto);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "name", target = "name")
    UserDto toUserDto(User user);

    List<UserDto> toUserDtoList(List<User> users);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    UserShortDto toUserShortDto(User user);

    List<UserShortDto> toUserShortDtoList(List<User> users);
}

