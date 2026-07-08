package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.CollectorClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.EventSort;
import ru.practicum.dto.event.PublicEventFilterParams;
import ru.practicum.event.facade.EventFacade;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventPublicController {
    private final EventFacade eventFacade;

    @GetMapping
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) EventSort sort,
            @PositiveOrZero @RequestParam(defaultValue = "0") int from,
            @Positive @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        log.info("EventPublicController: GET /events, from={}, size={}", from, size);

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        PublicEventFilterParams params = PublicEventFilterParams.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .build();
        return eventFacade.getPublicEvents(params, pageable);
    }

    @GetMapping("/{id}")
    public EventFullDto getEvent(
            @Positive @PathVariable Long id,
            @RequestHeader("X-EWM-USER-ID") Long userId
    ) {
        log.info("EventPublicController: GET /events/{}", id);
        return eventFacade.getPublicEvent(id, userId);
    }

    @GetMapping("/events/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId
    ) {
        return eventFacade.getRecommendations(userId);
    }

    @PutMapping("/events/{eventId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void likeEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") Long userId
    ) {
        eventFacade.likeEvent(userId, eventId);
    }
}
