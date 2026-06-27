package ru.practicum.client;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.config.FeignConfig;
import ru.practicum.client.fallback.RequestClientFallbackFactory;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service",
        configuration = FeignConfig.class,
        fallbackFactory = RequestClientFallbackFactory.class)
public interface RequestClient {

    @GetMapping("/internal/requests/confirmed-count")
    Map<Long, Integer> getConfirmedCountForEvents(@RequestParam List<Long> eventIds);

    @GetMapping("/internal/requests/confirmed-count/{eventId}")
    Integer getConfirmedCountForEvent(@Positive @PathVariable Long eventId);

    @GetMapping("/internal/requests/{eventId}")
    List<ParticipationRequestDto> getEventRequests(@Positive @PathVariable Long eventId);

    @GetMapping("/internal/requests")
    List<ParticipationRequestDto> getRequestsByIds(@RequestParam List<Long> requestIds);

    @PostMapping("/internal/requests/status")
    void updateRequestsStatus(@RequestParam Long eventId,
                              @RequestBody EventRequestStatusUpdateRequest request);
}
