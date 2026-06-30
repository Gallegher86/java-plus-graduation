package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.model.Request;

public interface RequestRepository extends
        JpaRepository<Request, Long>,
        RequestRepositoryCustom,
        QuerydslPredicateExecutor<Request> {
}
