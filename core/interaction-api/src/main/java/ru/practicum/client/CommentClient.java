package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.client.config.FeignConfig;
import ru.practicum.client.fallback.CommentClientFallbackFactory;
import ru.practicum.dto.comment.CommentEventDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "comment-service",
        configuration = FeignConfig.class,
        fallbackFactory = CommentClientFallbackFactory.class)
public interface CommentClient {

    @GetMapping("/internal/comments/{eventId}")
    List<CommentEventDto> getCommentsForEvent(@PathVariable("eventId") Long eventId);

    @GetMapping("/internal/comments")
    Map<Long, List<CommentEventDto>> getCommentsForEvents(@RequestParam List<Long> eventIds);
}
