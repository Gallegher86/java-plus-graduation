package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.facade.RequestFacade;

import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class RequestInternalController {
    private final RequestFacade requestFacade;

    @GetMapping("/confirmed-count")
    public Map<Long, Integer> getConfirmedCountForEvents(@RequestParam List<Long> eventIds) {
        log.info("RequestInternalController запрос на подсчет заявок по событиям: {}", eventIds);
        return requestFacade.getConfirmedCountForEvents(eventIds);
    }

    @GetMapping("/confirmed-count/{eventId}")
    public Integer getConfirmedCountForEvent(@Positive @PathVariable Long eventId) {
        log.info("RequestInternalController запрос на подсчет заявок по событию: {}", eventId);
        return requestFacade.getConfirmedCountForEvent(eventId);
    }

    @GetMapping("/{eventId}")
    public List<ParticipationRequestDto> getEventRequests(@Positive @PathVariable Long eventId) {
        log.info("RequestInternalController запрос на просмотр заявок по событию: {}", eventId);
        return requestFacade.getEventRequests(eventId);
    }

    @GetMapping
    public List<ParticipationRequestDto> getRequestsByIds(@RequestParam List<Long> requestIds) {
        log.info("RequestInternalController запрос на просмотр заявок по их ID: {}", requestIds);
        return requestFacade.getRequestsByIds(requestIds);
    }

    @PostMapping("/status")
    public void updateRequestsStatus(@RequestParam Long eventId,
                              @RequestBody EventRequestStatusUpdateRequest request) {
        log.info("RequestInternalController запрос на обновление статуса заявок с IDs {} в статус: {}",
                request.getRequestIds(), request.getStatus());
        requestFacade.updateRequestsStatus(eventId, request);
    }
}