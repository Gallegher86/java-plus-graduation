package ru.practicum.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {
    @NotBlank()
    @Size(min = 20, max = 2000, message = "Аннотация события должна включать от 20 до 2000 символов.")
    private String annotation;

    @NotNull()
    private Integer category;

    @NotBlank()
    @Size(min = 20, max = 7000, message = "Описание события должно включать от 20 до 7000 символов.")
    private String description;

    @NotNull()
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @NotNull()
    private Location location;

    private boolean paid = false;

    @PositiveOrZero
    private Integer participantLimit = 0;

    private boolean requestModeration = true;

    @NotBlank()
    @Size(min = 3, max = 120, message = "Название события должно включать от 3 до 120 символов.")
    private String title;
}
