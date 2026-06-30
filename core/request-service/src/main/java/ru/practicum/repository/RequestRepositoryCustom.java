package ru.practicum.repository;

import ru.practicum.dto.request.RequestStatus;
import ru.practicum.model.Request;

import java.util.List;
import java.util.Map;

public interface RequestRepositoryCustom {
    int confirmedCountForEvent(Long eventId);

    Map<Long, Integer> confirmedCountForEvents(List<Long> eventIds);

    List<Request> findAllByEventId(Long eventId);

    List<Request> findRequestsByIds(List<Long> ids);

    List<Request> findAllByRequesterId(Long requesterId);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    List<Request> findRequestsByStatusAndEventId(RequestStatus status, Long eventId);
}
