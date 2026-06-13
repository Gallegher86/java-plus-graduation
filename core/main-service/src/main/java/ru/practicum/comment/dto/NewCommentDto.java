package ru.practicum.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCommentDto {
    @NotBlank(message = "Комментарий не может быть пустым.")
    @Size(min = 3, max = 2000, message = "Комментарий должен включать от 3 до 2000 символов.")
    private String text;

    public void setText(String text) {
        this.text = text == null ? null : text.trim();
    }
}
