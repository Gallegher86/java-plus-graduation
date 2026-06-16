package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentShortDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.service.CommentService;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/comments")
public class CommentPrivateController {

    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentShortDto createComment(
            @Positive @PathVariable Long userId,
            @Positive @RequestParam Long eventId,
            @RequestBody @Valid NewCommentDto dto
    ) {
        log.info("CommentPrivateController POST /users/{}/comments, event: {}, dto: {}",
                userId, eventId,  dto);
        return commentService.createComment(userId, eventId, dto);
    }

    @PatchMapping("/{commentId}")
    public CommentShortDto updateComment(
            @Positive @PathVariable Long userId,
            @Positive @PathVariable Long commentId,
            @RequestBody @Valid NewCommentDto dto
    ) {
        log.info("CommentPrivateController PATCH /users/{}/comments/{}, dto: {}",
                userId, commentId, dto);
        return commentService.updateComment(userId, commentId, dto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Positive @PathVariable Long userId,
            @Positive @PathVariable Long commentId) {
        log.info("CommentPrivateController DELETE /users/{}/comments/{}", userId, commentId);
        commentService.deleteByUser(userId, commentId);
    }
}
