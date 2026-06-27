package ru.practicum.facade;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.client.EventClient;
import ru.practicum.dto.event.EventInternalDto;
import ru.practicum.exception.ServiceUnavailableException;

@Component
@RequiredArgsConstructor
public class EventClientFacade {

    private final EventClient eventClient;

    @CircuitBreaker(name = "eventService", fallbackMethod = "getEventFallback")
    public EventInternalDto getEvent(Long id) {
        return eventClient.getEvent(id);
    }

    public EventInternalDto getEventFallback(Long id, Throwable t) {
        throw new ServiceUnavailableException("event-service недоступен", t);
    }
}
