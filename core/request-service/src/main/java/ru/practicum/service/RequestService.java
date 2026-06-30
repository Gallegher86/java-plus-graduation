package ru.practicum.service;

import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.model.Request;

import java.util.List;
import java.util.Map;

public interface RequestService {
    ParticipationRequestDto create(Request request);

    List<ParticipationRequestDto> findAll(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    int getConfirmedCountForEvent(Long eventId);

    Map<Long, Integer> getConfirmedCountForEvents(List<Long> eventIds);

    List<ParticipationRequestDto> getEventRequests(Long eventId);

    List<ParticipationRequestDto> getRequestsByIds(List<Long> requestIds);

    void updateRequestsStatus(Long eventId, EventRequestStatusUpdateRequest request);

    boolean checkRequestExists(Long userId, Long eventId);
}
