package ru.practicum.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.practicum.user.model.User;

import java.util.List;

public interface UserRepositoryCustom {
    Page<User> findByIdInWithPagination(List<Long> ids, Pageable pageable);

    long deleteByIdCustom(Long id);
}
