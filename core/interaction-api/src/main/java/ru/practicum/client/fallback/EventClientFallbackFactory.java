package ru.practicum.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.client.EventClient;
import ru.practicum.dto.event.EventInternalDto;
import ru.practicum.exception.ServiceUnavailableException;

import java.util.List;

@Slf4j
@Component
public class EventClientFallbackFactory implements FallbackFactory<EventClient> {

    @Override
    public EventClient create(Throwable cause) {

        log.error("event-service недоступен или вызов завершился ошибкой", cause);

        return new EventClient() {

            @Override
            public EventInternalDto getEvent(Long eventId) {
                throw new ServiceUnavailableException("event-service недоступен", cause);
            }

            @Override
            public List<EventInternalDto> getEvents(List<Long> ids) {
                throw new ServiceUnavailableException("event-service недоступен", cause);
            }
        };
    }
}
