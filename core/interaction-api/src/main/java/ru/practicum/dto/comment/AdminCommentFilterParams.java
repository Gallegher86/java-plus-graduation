package ru.practicum.dto.comment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder(toBuilder = true)
public class AdminCommentFilterParams {
    private Set<Long> comments;
    private Set<Long> authors;
    private Set<Long> events;
    private Set<CommentState> states;
    private String text;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
}
