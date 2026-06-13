package ru.practicum.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewCategoryDto {
    @NotBlank(message = "Название категории мероприятий должно быть указано.")
    @Size(min = 3, max = 50, message = "Название категории мероприятий должно содержать от 3 до 50 символов.")
    private String name;

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }
}