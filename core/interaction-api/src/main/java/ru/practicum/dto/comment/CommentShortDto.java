package ru.practicum.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentShortDto {
    private Long id;
    private LocalDateTime createdOn;
    private String text;
    private Long authorId;
    private Long eventId;
    private CommentState state;
}
