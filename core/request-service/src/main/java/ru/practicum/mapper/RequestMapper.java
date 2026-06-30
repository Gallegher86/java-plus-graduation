package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.model.Request;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", imports = {LocalDateTime.class, RequestStatus.class})
public interface RequestMapper {
    @Mapping(target = "event", source = "eventId")
    @Mapping(target = "requester", source = "requesterId")
    ParticipationRequestDto toParticipantRequestDto(Request request);
}
