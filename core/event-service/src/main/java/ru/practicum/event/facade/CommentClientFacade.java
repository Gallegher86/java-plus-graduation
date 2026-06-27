package ru.practicum.event.facade;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.client.CommentClient;
import ru.practicum.dto.comment.CommentEventDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentClientFacade {

    private final CommentClient commentClient;

    @CircuitBreaker(name = "commentService", fallbackMethod = "getCommentsForEventFallback")
    public List<CommentEventDto> getCommentsForEvent(Long id) {
        return commentClient.getCommentsForEvent(id);
    }

    @CircuitBreaker(name = "commentService", fallbackMethod = "getCommentsForEventsFallback")
    public Map<Long, List<CommentEventDto>> getCommentsForEvents(List<Long> eventIds) {
        return commentClient.getCommentsForEvents(eventIds);
    }

    public List<CommentEventDto> getCommentsForEventFallback(Long id, Throwable t) {
        log.warn("comment-service недоступен, возвращаю пустой список комментариев для события {}", id, t);
        return Collections.emptyList();
    }

    public Map<Long, List<CommentEventDto>> getCommentsForEventsFallback(List<Long> eventIds, Throwable t) {
        log.warn("comment-service недоступен, возвращаю пустую мапу комментариев для событий {}", eventIds, t);
        return eventIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        id -> List.of()
                ));
    }
}
