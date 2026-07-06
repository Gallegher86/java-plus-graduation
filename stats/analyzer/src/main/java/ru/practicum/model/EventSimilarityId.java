package ru.practicum.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EventSimilarityId implements Serializable {
    @Column(name = "event_a")
    private Long eventA;

    @Column(name = "event_b")
    private Long eventB;
}
