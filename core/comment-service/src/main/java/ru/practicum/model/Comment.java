package ru.practicum.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.practicum.dto.comment.CommentState;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_comments", schema = "public")
@Getter
@Setter
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private CommentState state;
}
