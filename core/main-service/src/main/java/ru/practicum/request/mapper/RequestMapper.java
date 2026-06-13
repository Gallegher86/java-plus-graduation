package ru.practicum.request.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.event.model.Event;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.Status;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring", imports = {LocalDateTime.class, Status.class})
public interface RequestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", expression = "java(LocalDateTime.now())")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "requester", source = "user")
    @Mapping(target = "status", expression = "java(Status.PENDING)")
    Request toRequest(User user, Event event);

    @Mapping(target = "event", source = "event.id")
    @Mapping(target = "requester", source = "requester.id")
    ParticipationRequestDto toParticipantRequestDto(Request request);

    List<ParticipationRequestDto> toParticipantRequestDto(List<Request> requests);
}