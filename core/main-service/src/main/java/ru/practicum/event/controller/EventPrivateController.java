package ru.practicum.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.*;
import ru.practicum.event.service.EventService;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
public class EventPrivateController {

    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(
            @Positive @PathVariable Long userId,
            @RequestBody @Valid NewEventDto newEventDto
    ) {
        log.info("EventPrivateController: POST /users/{}/events", userId);
        return eventService.createEvent(userId, newEventDto);
    }

    @GetMapping
    public List<EventShortDto> getUserEvents(
            @Positive @PathVariable Long userId,
            @PositiveOrZero @RequestParam(defaultValue = "0") int from,
            @Positive @RequestParam(defaultValue = "10") int size
    ) {
        log.info("EventPrivateController: GET /users/{}/events, from={}, size={}", userId, from, size);
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);
        return eventService.getUserEvents(userId, pageable);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getUserEvent(
            @Positive @PathVariable Long userId,
            @Positive @PathVariable Long eventId
    ) {
        log.info("EventPrivateController: GET /users/{}/events/{}", userId, eventId);
        return eventService.getUserEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateUserEvent(
            @Positive @PathVariable Long userId,
            @Positive @PathVariable Long eventId,
            @RequestBody @Valid UpdateEventUserRequest request
    ) {
        log.info("EventPrivateController: PATCH /users/{}/events/{}", userId, eventId);
        return eventService.updateEventByUser(userId, eventId, request);
    }

    //Получение заявок на участие в событии текущего пользователя
    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(
            @Positive @PathVariable Long userId,
            @Positive @PathVariable Long eventId
    ) {
        log.info("EventPrivateController: GET /users/{}/events/{}/requests", userId, eventId);
        return eventService.getEventRequests(userId, eventId);
    }

    //Изменение статуса заявок (CONFIRMED / REJECTED)
    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateEventRequestsStatus(
            @Positive @PathVariable Long userId,
            @Positive @PathVariable Long eventId,
            @RequestBody @Valid EventRequestStatusUpdateRequest request
    ) {
        log.info("EventPrivateController: PATCH /users/{}/events/{}/requests", userId, eventId);
        return eventService.updateEventRequestsStatus(userId, eventId, request);
    }
}