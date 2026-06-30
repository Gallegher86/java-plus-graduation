package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comment.CommentEventDto;
import ru.practicum.facade.CommentFacade;

import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/comments")
public class CommentInternalController {
    private final CommentFacade commentFacade;

    @GetMapping("/{eventId}")
    public List<CommentEventDto> getCommentsForEvent(@Positive @PathVariable Long eventId) {
        return commentFacade.getCommentsForEvent(eventId);
    }

    @GetMapping
    public Map<Long, List<CommentEventDto>> getCommentsForEvents(@RequestParam List<Long> eventIds) {
        return commentFacade.getCommentsForEvents(eventIds);
    }
}
