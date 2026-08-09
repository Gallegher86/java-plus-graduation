package ru.practicum.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "event_similarity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSimilarity {
    @EmbeddedId
    private EventSimilarityId id;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private Instant timestamp;
}
