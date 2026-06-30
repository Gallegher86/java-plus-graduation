package ru.practicum.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.dto.event.EventInternalDto;
import ru.practicum.dto.user.UserDto;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentFullDto {
    private Long id;
    private LocalDateTime createdOn;
    private String text;
    private UserDto author;
    private EventInternalDto event;
    private CommentState state;
}
