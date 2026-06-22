package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.model.Request;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {
    boolean existsByEventIdAndRequesterId(Long eventId, Long userId);

    List<Request> findAllByRequesterId(Long userId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);
}
