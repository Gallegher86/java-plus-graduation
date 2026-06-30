package ru.practicum.dto.event;

import lombok.Builder;
import lombok.Data;
import ru.practicum.dto.category.CategoryDto;

import java.time.LocalDateTime;

@Data
@Builder
public class EventInternalDto {
    private String annotation;
    private CategoryDto category;
    private int participantLimit;
    private LocalDateTime eventDate;
    private Long id;
    private Long initiatorId;
    private EventState state;
    private boolean requestModeration;
}
