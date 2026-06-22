package ru.practicum.facade;

import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

public interface RequestFacade {
    ParticipationRequestDto create(Long userId, Long eventId);

    List<ParticipationRequestDto> findAll(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);
}
