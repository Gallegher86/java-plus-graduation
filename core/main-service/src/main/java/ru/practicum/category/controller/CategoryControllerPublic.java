package ru.practicum.category.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.service.CategoryService;
import ru.practicum.category.dto.CategoryDto;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping(path = "/categories")
@RequiredArgsConstructor
public class CategoryControllerPublic {
    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDto> get(@PositiveOrZero @RequestParam(name = "from", defaultValue = "0") int from,
                                 @Positive @RequestParam(name = "size", defaultValue = "10") int size) {
        log.info("CategoryControllerPublic: получен запрос на просмотр списка категорий, " +
                "начиная с: {}, размером : {}.", from, size);

        Pageable pageable = makePageable(from, size);
        return categoryService.get(pageable);
    }

    @GetMapping("/{catId}")
    public CategoryDto getById(@Positive @PathVariable Long catId) {
        log.info("CategoryControllerPublic: получен запрос на просмотр категории с id: {}.", catId);
        return categoryService.getById(catId);
    }

    private Pageable makePageable(Integer from, Integer size) {
        return PageRequest.of(
                from / size,
                size);
    }
}
