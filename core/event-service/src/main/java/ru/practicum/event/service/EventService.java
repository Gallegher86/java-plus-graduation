package ru.practicum.event.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.event.*;
import ru.practicum.event.model.Event;

import java.util.List;

public interface EventService {

    // PUBLIC

    List<Event> getPublicEvents(PublicEventFilterParams params, Pageable pageable);

    Event getPublicEvent(Long eventId);


    // ADMIN

    List<Event> getAdminEvents(AdminEventFilterParams params, Pageable pageable);

    Event updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);


    // PRIVATE (user)

    Event createEvent(Long userId, NewEventDto newEventDto);

    List<Event> getUserEvents(Long userId, Pageable pageable);

    Event getUserEvent(Long userId, Long eventId);

    Event updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request);

    Event getEventForRequestsOrThrow(Long userId, Long eventId);
}
