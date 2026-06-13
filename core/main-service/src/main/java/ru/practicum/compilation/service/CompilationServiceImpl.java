package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.EntityFinder;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.NotFoundException;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EntityFinder entityFinder;

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto dto) {
        log.info("CompilationService: получен запрос на создание компиляции: {}.", dto);
        Set<Event> events = getEvents(dto.getEvents());
        Compilation entity = compilationMapper.toCompilation(dto, events);
        Compilation saved = compilationRepository.save(entity);
        CompilationDto result = compilationMapper.toCompilationDto(saved);
        log.info("CompilationService: компиляция сохранена: {}", result);
        return result;
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        log.info("CompilationService: получен запрос на удаление компиляции с id: {}.", compId);
        Compilation compilation = entityFinder.getCompilationOrThrow(compId);
        compilationRepository.delete(compilation);
        log.info("CompilationService: компиляция с id: {} удалена.", compId);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest dto) {
        log.info("CompilationService: получен запрос на обновление компиляции с id {}.", compId);
        Compilation saved = entityFinder.getCompilationOrThrow(compId);
        updateFields(saved, dto);
        CompilationDto result = compilationMapper.toCompilationDto(saved);
        log.info("CompilationService: категория обновлена: {}", result);
        return result;
    }

    @Override
    public List<CompilationDto> get(Pageable pageable, Boolean pinned) {
        log.info("CompilationService: получен запрос на получение списка компиляций.");
        Page<Long> pageIds = compilationRepository.findCompilationIds(pinned, pageable);

        List<Compilation> saved = pageIds.isEmpty()
                ? List.of()
                : compilationRepository.findAllCompilationsWithEvents(pageIds.getContent());

        List<CompilationDto> result = saved.stream().map(compilationMapper::toCompilationDto).toList();
        log.info("CompilationService: выдана страница компиляций размером: {}, начиная с {}.",
                saved.size(), pageable.getOffset());
        return result;
    }

    @Override
    public CompilationDto getById(Long compId) {
        log.info("CompilationService: получен запрос на получение компиляции с id: {}.", compId);
        Compilation saved = entityFinder.getCompilationOrThrow(compId);
        CompilationDto result = compilationMapper.toCompilationDto(saved);
        log.info("CompilationService: категория выдана: {}", result);
        return result;
    }

    private Set<Event> getEvents(Set<Long> eventIds) {
        Set<Event> result = new HashSet<>();

        if (eventIds != null && !eventIds.isEmpty()) {
            result.addAll(eventRepository.findAllWithCategoryAndInitiator(eventIds));

            if (eventIds.size() > result.size()) {
                throw new NotFoundException("В полученном списке событий есть не существующие.");
            }
        }
        return result;
    }

    private void updateFields(Compilation compilation, UpdateCompilationRequest dto) {
        if (dto.getEvents() != null) {
            compilation.setEvents(getEvents(dto.getEvents()));
        }

        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }

        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }
    }
}
