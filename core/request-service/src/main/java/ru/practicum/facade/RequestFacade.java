package ru.practicum.facade;

import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

public interface RequestFacade {
    ParticipationRequestDto create(Long userId, Long eventId);

    List<ParticipationRequestDto> findAll(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    Map<Long, Integer> getConfirmedCountForEvents(List<Long> eventIds);

    Integer getConfirmedCountForEvent(Long eventId);

    List<ParticipationRequestDto> getEventRequests(Long eventId);

    List<ParticipationRequestDto> getRequestsByIds(List<Long> requestIds);

    void updateRequestsStatus(Long eventId, EventRequestStatusUpdateRequest request);
}
