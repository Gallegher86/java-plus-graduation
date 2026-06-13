package ru.practicum.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewUserRequest {
    @NotBlank()
    @Email(message = "wrong format")
    @Length(min = 6, max = 254, message = "Email length must be between 2 and 250 characters")
    private String email;

    @NotBlank()
    @Length(min = 2, max = 250, message = "Name length must be between 2 and 250 characters")
    private String name;
}