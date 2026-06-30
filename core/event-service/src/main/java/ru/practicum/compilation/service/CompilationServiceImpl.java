package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.NotFoundException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    @Transactional
    public CompilationDto create(Compilation entity) {
        Compilation saved = compilationRepository.save(entity);
        return compilationMapper.toCompilationDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        log.info("CompilationService: получен запрос на удаление компиляции с id: {}.", compId);
        Compilation compilation = getCompilationOrThrow(compId);
        compilationRepository.delete(compilation);
        log.info("CompilationService: компиляция с id: {} удалена.", compId);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest dto) {
        Compilation saved = getCompilationOrThrow(compId);
        updateFields(saved, dto);
        return compilationMapper.toCompilationDto(saved);
    }

    @Override
    public List<Compilation> get(Pageable pageable, Boolean pinned) {
        Page<Long> pageIds = compilationRepository.findCompilationIds(pinned, pageable);

        if (pageIds.isEmpty()) {
            return List.of();
        }

        return compilationRepository.findAllCompilationsWithEvents(pageIds.getContent());
    }

    @Override
    public Compilation getById(Long compId) {
        return getCompilationOrThrow(compId);
    }

    @Override
    public Set<Event> getEvents(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Event> events = new HashSet<>(eventRepository.findAllWithCategory(eventIds));

        Set<Long> foundIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toSet());

        Set<Long> missingIds = new HashSet<>(eventIds);
        missingIds.removeAll(foundIds);

        if (!missingIds.isEmpty()) {
            throw new NotFoundException("Не найдены события с id: " + missingIds);
        }

        return events;
    }

    public Compilation getCompilationOrThrow(Long id) {
        return compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Компиляция с id=" + id + " не найдена"));
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
