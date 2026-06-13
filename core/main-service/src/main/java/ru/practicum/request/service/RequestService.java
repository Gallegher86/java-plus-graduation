package ru.practicum.request.service;

import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestService {
    ParticipationRequestDto create(Long userId, Long eventId);

    List<ParticipationRequestDto> findAll(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);
}