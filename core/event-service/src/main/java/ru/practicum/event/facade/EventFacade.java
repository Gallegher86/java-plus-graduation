package ru.practicum.event.facade;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

public interface EventFacade {

    // PUBLIC

    List<EventShortDto> getPublicEvents(PublicEventFilterParams params, Pageable pageable);

    EventFullDto getPublicEvent(Long eventId);


    // ADMIN

    List<EventFullDto> getAdminEvents(AdminEventFilterParams params, Pageable pageable);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);


    // PRIVATE (user)

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    List<EventShortDto> getUserEvents(Long userId, Pageable pageable);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateEventRequestsStatus(Long userId,
                                                             Long eventId,
                                                             EventRequestStatusUpdateRequest request);

    // INTERNAl

    EventInternalDto getEvent(Long eventId);

    List<EventInternalDto> getEvents(List<Long> ids);
}
