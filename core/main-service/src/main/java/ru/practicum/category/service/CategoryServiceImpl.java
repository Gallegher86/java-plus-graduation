package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.common.EntityFinder;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper categoryMapper;
    private final EntityFinder entityFinder;

    @Override
    public CategoryDto create(NewCategoryDto dto) {
        log.info("CategoryService: получен запрос на создание категории: {}.", dto);
        validateName(dto.getName());
        Category entity = categoryMapper.toCategory(dto);
        Category saved = categoryRepository.save(entity);
        CategoryDto result = categoryMapper.toCategoryDto(saved);
        log.info("CategoryService: категория сохранена: {}", result);
        return result;
    }

    @Override
    @Transactional
    public void delete(Long catId) {
        log.info("CategoryService: получен запрос на удаление категории с id: {}.", catId);

        Category category = entityFinder.getCategoryOrThrow(catId);

        if (eventRepository.existsByCategoryId(catId)) {
            String errorMessage = String.format("Нельзя удалить категорию с id: %d, есть связанные события.", catId);
            throw new ConflictException(errorMessage);
        }

        categoryRepository.delete(category);
        log.info("CategoryService: категория с id: {} удалена.", catId);
    }

    @Override
    @Transactional
    public CategoryDto update(Long catId, NewCategoryDto dto) {
        log.info("CategoryService: получен запрос на обновление категории с id {}, новое имя: {}.",
                catId, dto.getName());

        Category saved = entityFinder.getCategoryOrThrow(catId);
        String newName = dto.getName();

        if (newName.equals(saved.getName())) {
            CategoryDto result = categoryMapper.toCategoryDto(saved);
            return result;
        }

        validateName(newName);
        saved.setName(newName);
        Category updated = categoryRepository.save(saved);
        CategoryDto result = categoryMapper.toCategoryDto(updated);
        log.info("CategoryService: категория обновлена: {}", result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> get(Pageable pageable) {
        log.info("CategoryService: получен запрос на получение списка категорий.");
        List<Category> saved = categoryRepository.findCategoriesWithPagination(pageable);
        List<CategoryDto> result = categoryMapper.toCategoryDtoList(saved);
        log.info("CategoryService: выдана страница категорий размером: {}, начиная с {}.",
                saved.size(), pageable.getOffset());
        return result;
    }

    @Override
    public CategoryDto getById(Long catId) {
        log.info("CategoryService: получен запрос на получение категории с id: {}.", catId);
        Category saved = entityFinder.getCategoryOrThrow(catId);
        CategoryDto result = categoryMapper.toCategoryDto(saved);
        log.info("CategoryService: категория выдана: {}", result);
        return result;
    }

    private void validateName(String name) {
        String trimmed = name.trim();

        if (categoryRepository.existsByNameIgnoreCase(trimmed)) {
            String errorMessage = String.format("CategoryService: категория %s уже существует.", name);
            throw new ConflictException(errorMessage);
        }
    }
}
