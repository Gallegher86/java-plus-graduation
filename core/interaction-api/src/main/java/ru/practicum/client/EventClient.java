package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.client.config.FeignConfig;
import ru.practicum.dto.event.EventCommentDto;

import java.util.List;

@FeignClient(name = "event-service", configuration = FeignConfig.class)
public interface EventClient {

    @GetMapping("/internal/events/{eventId}")
    EventCommentDto getEvent(@PathVariable("eventId") Long eventId);

    @GetMapping("/internal/events")
    List<EventCommentDto> getEvents(@RequestParam List<Long> ids);
}
