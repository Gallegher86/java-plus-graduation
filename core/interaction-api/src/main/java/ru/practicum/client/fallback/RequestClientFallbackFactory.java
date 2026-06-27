package ru.practicum.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
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
public class RequestClientFallbackFactory implements FallbackFactory<RequestClient> {

    @Override
    public RequestClient create(Throwable cause) {
        return new RequestClient() {

            @Override
            public Map<Long, Integer> getConfirmedCountForEvents(List<Long> eventIds) {
                log.error("request-service недоступна. Возвращаю мапу с нулевыми запросами", cause);

                return eventIds.stream()
                        .collect(Collectors.toMap(
                                Function.identity(),
                                id -> 0
                        ));
            }

            @Override
            public Integer getConfirmedCountForEvent(Long eventId) {
                log.error("request-service недоступна. Возвращаю 0 запросов дл события {}", eventId, cause);
                return 0;
            }

            @Override
            public List<ParticipationRequestDto> getEventRequests(Long eventId) {
                throw serviceUnavailable(cause);
            }

            @Override
            public List<ParticipationRequestDto> getRequestsByIds(List<Long> requestIds) {
                throw serviceUnavailable(cause);
            }

            @Override
            public void updateRequestsStatus(Long eventId, EventRequestStatusUpdateRequest request) {
                throw serviceUnavailable(cause);
            }

            private ServiceUnavailableException serviceUnavailable(Throwable cause) {
                log.error("request-service недоступна", cause);
                return new ServiceUnavailableException("request-service недоступен", cause);
            }
        };
    }
}
