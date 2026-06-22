package ru.practicum.service;

import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.model.Request;

import java.util.List;

public interface RequestService {
    ParticipationRequestDto create(Request request);

    List<ParticipationRequestDto> findAll(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    int getConfirmedCount(Long eventId);

    boolean checkRequestExists(Long userId, Long eventId);
}
