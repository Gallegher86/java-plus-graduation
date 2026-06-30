package ru.practicum.event.facade;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.client.RequestClient;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.exception.ServiceUnavailableException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestClientFacade {

    private final RequestClient requestClient;

    @CircuitBreaker(name = "requestService", fallbackMethod = "getConfirmedCountForEventFallback")
    public int getConfirmedCountForEvent(Long id) {
        return requestClient.getConfirmedCountForEvent(id);
    }

    @CircuitBreaker(name = "requestService", fallbackMethod = "getConfirmedCountForEventsFallback")
    public Map<Long, Integer> getConfirmedCountForEvents(List<Long> ids) {
        return requestClient.getConfirmedCountForEvents(ids);
    }

    @CircuitBreaker(name = "requestService", fallbackMethod = "getRequestsByIdsFallback")
    public List<ParticipationRequestDto> getRequestsByIds(List<Long> ids) {
        return requestClient.getRequestsByIds(ids);
    }

    @CircuitBreaker(name = "requestService", fallbackMethod = "getEventRequestsFallback")
    public List<ParticipationRequestDto> getEventRequests(Long id) {
        return requestClient.getEventRequests(id);
    }

    @CircuitBreaker(name = "requestService", fallbackMethod = "updateRequestsStatusFallback")
    public void updateRequestsStatus(Long id, EventRequestStatusUpdateRequest req) {
        requestClient.updateRequestsStatus(id, req);
    }

    public int getConfirmedCountForEventFallback(Long id, Throwable t) {
        log.warn("Не удалось получить количество подтвержденных заявок для события {}, возвращаю 0", id, t);
        return 0;
    }

    public Map<Long, Integer> getConfirmedCountForEventsFallback(List<Long> ids, Throwable t) {
        log.warn("request-service недоступен, возвращаю 0 подтвержденных заявок для событий {}", ids, t);

        return ids.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        id -> 0
                ));
    }

    public List<ParticipationRequestDto> getEventRequestsFallback(Long id, Throwable t) {
        throw new ServiceUnavailableException("request-service недоступен", t);
    }

    public List<ParticipationRequestDto> getRequestsByIdsFallback(List<Long> ids, Throwable t) {
        throw new ServiceUnavailableException("request-service недоступен", t);
    }

    public void updateRequestsStatusFallback(Long id, EventRequestStatusUpdateRequest req, Throwable t) {
        throw new ServiceUnavailableException("request-service недоступен", t);
    }
}
