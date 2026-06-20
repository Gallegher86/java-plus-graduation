package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comment.*;
import ru.practicum.facade.CommentFacade;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/comments")
public class CommentAdminController {
    private final CommentFacade commentFacade;

    @PatchMapping
    public List<CommentShortDto> approveComments(@RequestBody @Valid CommentStatusUpdateRequest dto) {
        log.info("CommentAdminController PATCH /admin/comments, dto: {}", dto);
        return commentFacade.approveComments(dto);
    }

    @GetMapping
    public List<CommentFullDto> getComments(
            @RequestParam(required = false) Set<Long> comments,
            @RequestParam(required = false) Set<Long> authors,
            @RequestParam(required = false) Set<Long> events,
            @RequestParam(required = false) Set<CommentState> states,
            @RequestParam(required = false) String text,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @PositiveOrZero @RequestParam(defaultValue = "0") int from,
            @Positive @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) CommentSort sort
    ) {
        log.info("CommentAdminController GET /admin/comments");

        AdminCommentFilterParams params = AdminCommentFilterParams.builder()
                .comments(comments)
                .authors(authors)
                .events(events)
                .states(states)
                .text(text)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .build();

        Pageable pageable = makePageable(from, size, sort);

        return commentFacade.getComments(params, pageable);
    }

    @GetMapping("/{commentId}")
    public CommentFullDto getCommentById(@Positive @PathVariable Long commentId) {
        log.info("CommentAdminController GET /admin/comments/{}", commentId);
        return commentFacade.getCommentById(commentId);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Positive @PathVariable Long commentId) {
        log.info("CommentAdminController DELETE /admin/comments/{}", commentId);
        commentFacade.deleteByAdmin(commentId);
    }

    private Pageable makePageable(int from, int size, CommentSort sort) {
        int page = from / size;

        if (sort == null) {
            sort = CommentSort.COMMENT_ID;
        }

        Sort sortBy = switch (sort) {
            case COMMENT_DATE -> Sort.by(Sort.Direction.ASC, "createdOn");
            case COMMENT_ID   -> Sort.by(Sort.Direction.ASC, "id");
            case AUTHOR_ID    -> Sort.by(Sort.Direction.ASC, "author.id");
            case EVENT_ID     -> Sort.by(Sort.Direction.ASC, "event.id");
        };

        return PageRequest.of(page, size, sortBy);
    }
}
