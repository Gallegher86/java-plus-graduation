package ru.practicum.category.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto create(NewCategoryDto dto);

    void delete(Long catId);

    CategoryDto update(Long catId, NewCategoryDto dto);

    List<CategoryDto> get(Pageable pageable);

    CategoryDto getById(Long catId);
}
