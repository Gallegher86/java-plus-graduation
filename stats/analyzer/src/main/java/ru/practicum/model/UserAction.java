package ru.practicum.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_actions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAction {
    @EmbeddedId
    private UserActionId id;

    @Column(nullable = false)
    private Double weight;

    @Column(nullable = false)
    private Instant timestamp;
}
