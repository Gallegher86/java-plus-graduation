package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventInternalDto;
import ru.practicum.event.facade.EventFacade;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/events")
public class EventInternalController {
    private final EventFacade eventFacade;

    @GetMapping("/{eventId}")
    public EventInternalDto getEvent(@PathVariable("eventId") Long eventId) {
        return eventFacade.getEvent(eventId);
    }

    @GetMapping
    public List<EventInternalDto> getEvents(@RequestParam List<Long> ids) {
        return eventFacade.getEvents(ids);
    }
}
