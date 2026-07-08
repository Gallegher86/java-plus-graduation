package ru.practicum.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.event.EventState;
import ru.practicum.event.model.Event;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {
    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT e FROM Event e " +
            "LEFT JOIN FETCH e.category " +
            "WHERE e.id in :ids")
    List<Event> findAllWithCategory(@Param("ids") Set<Long> ids);

    @Query("""
    SELECT e
    FROM Event e
    LEFT JOIN FETCH e.category
    WHERE e.id IN :ids
      AND e.state = :state
    """)
    List<Event> findAllWithCategoryByIdInAndState(@Param("ids") Collection<Long> ids,
                                                  @Param("state") EventState state);
}
