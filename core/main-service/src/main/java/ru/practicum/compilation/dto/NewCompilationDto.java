package ru.practicum.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {
    private Set<Long> events;

    @Builder.Default
    private Boolean pinned = false;

    @NotBlank(message = "Компиляция должна иметь название.")
    @Size(min = 3, max = 50, message = "Имя компиляции должно содержать от 3 до 50 символов.")
    private String title;
}
