package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.client.config.FeignConfig;
import ru.practicum.dto.event.EventInternalDto;

import java.util.List;

@FeignClient(name = "event-service", configuration = FeignConfig.class)
public interface EventClient {

    @GetMapping("/internal/events/{eventId}")
    EventInternalDto getEvent(@PathVariable("eventId") Long eventId);

    @GetMapping("/internal/events")
    List<EventInternalDto> getEvents(@RequestParam List<Long> ids);

    @GetMapping("/internal/events/check/{eventId}")
    void checkEvent(@PathVariable("eventId") Long userId);
}
