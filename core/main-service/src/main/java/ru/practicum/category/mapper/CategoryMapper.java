package ru.practicum.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.model.Category;

import java.util.List;

/**
 * Маппер для преобразования между DTO категорий и сущностью Category.
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    /**
     * Преобразует NewCategoryDto в сущность Category.
     */
    @Mapping(target = "id", ignore = true)
    Category toCategory(NewCategoryDto dto);

    /**
     * Преобразует сущность Category в CategoryDto.
     */
    CategoryDto toCategoryDto(Category category);

    /**
     * Преобразует список сущностей Category в список DTO CategoryDto.
     */
    List<CategoryDto> toCategoryDtoList(List<Category> categories);
}
