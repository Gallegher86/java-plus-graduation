package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.ScoredEvent;
import ru.practicum.model.UserAction;
import ru.practicum.model.UserActionId;

import java.util.List;

public interface UserActionRepository extends JpaRepository<UserAction, UserActionId> {
    @Query("""
    SELECT ua.id.eventId
    FROM UserAction ua
    WHERE ua.id.userId = :userId
    ORDER BY ua.timestamp DESC
""")
    List<Long> findRecentEventIdsByUserId(@Param("userId") long userId, Pageable pageable);

    @Query("""
    SELECT DISTINCT ua.id.eventId
    FROM UserAction ua
    WHERE ua.id.userId = :userId
""")
    List<Long> findAllInteractedEventIdsByUser(@Param("userId") long userId);

    @Query("""
    SELECT new ru.practicum.dto.ScoredEvent(
        ua.id.eventId,
        SUM(ua.weight)
    )
    FROM UserAction ua
    WHERE ua.id.eventId IN :eventIds
    GROUP BY ua.id.eventId
""")
    List<ScoredEvent> sumWeightsByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("""
    SELECT ua
    FROM UserAction ua
    WHERE ua.id.userId = :userId
""")
    List<UserAction> findAllByUserId(@Param("userId") long userId);
}
