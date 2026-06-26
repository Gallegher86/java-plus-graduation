package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.facade.RequestFacade;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class RequestInternalController {
    private final RequestFacade requestFacade;

    @GetMapping("/confirmed-count")
    public Map<Long, Integer> getConfirmedCountForEvents(@RequestParam List<Long> eventIds) {
        return requestFacade.getConfirmedCountForEvents(eventIds);
    }

    @GetMapping("/confirmed-/{eventId}")
    public Integer getConfirmedCountForEvent(@Positive @PathVariable Long eventId) {
        return requestFacade.getConfirmedCountForEvent(eventId);
    }

    @GetMapping("/internal/requests/{eventId}")
    public List<ParticipationRequestDto> getEventRequests(@Positive @PathVariable Long eventId) {
        return requestFacade.getEventRequests(eventId);
    }

    @GetMapping("/internal/requests")
    public List<ParticipationRequestDto> getRequestsByIds(@RequestParam List<Long> requestIds) {
        return requestFacade.getRequestsByIds(requestIds);
    }

    @PostMapping("/internal/requests/status")
    void updateRequestsStatus(@RequestParam Long eventId,
                              @RequestBody EventRequestStatusUpdateRequest request) {
        requestFacade.updateRequestsStatus(eventId, request);
    }
}