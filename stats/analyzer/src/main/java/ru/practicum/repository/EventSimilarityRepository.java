package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.EventSimilarityId;

import java.util.List;
import java.util.Set;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, EventSimilarityId> {

    @Query("""
    SELECT es
    FROM EventSimilarity es
    WHERE (
        es.id.eventA IN :recentEventIds
        AND es.id.eventB NOT IN :interactedEventIds
    )
    OR (
        es.id.eventB IN :recentEventIds
        AND es.id.eventA NOT IN :interactedEventIds
    )
    ORDER BY es.score DESC
""")
    List<EventSimilarity> findSimilarEvents(
            @Param("recentEventIds") Set<Long> recentEventIds,
            @Param("interactedEventIds") Set<Long> interactedEventIds,
            Pageable pageable
    );

    @Query("""
    SELECT es
    FROM EventSimilarity es
    WHERE (
            es.id.eventA = :candidateId
            AND es.id.eventB IN :interactedEventIds
          )
       OR (
            es.id.eventB = :candidateId
            AND es.id.eventA IN :interactedEventIds
          )
    ORDER BY es.score DESC
""")
    List<EventSimilarity> findNearestViewedEvents(
            @Param("candidateId") Long candidateId,
            @Param("interactedEventIds") Set<Long> interactedEventIds,
            Pageable pageable);

    @Query("""
    SELECT es
    FROM EventSimilarity es
    WHERE (
            es.id.eventA = :eventId
            AND es.id.eventB NOT IN :interactedEventIds
          )
       OR (
            es.id.eventB = :eventId
            AND es.id.eventA NOT IN :interactedEventIds
          )
    ORDER BY es.score DESC
""")
    List<EventSimilarity> findByEvent(
            @Param("eventId") long eventId,
            @Param("interactedEventIds") Set<Long> interactedEventIds,
            Pageable pageable);
}
