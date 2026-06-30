package ru.practicum.compilation.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.event.model.Event;

import java.util.List;
import java.util.Set;

public interface CompilationService {
    CompilationDto create(Compilation entity);

    void delete(Long compId);

    CompilationDto update(Long compId, UpdateCompilationRequest dto);

    List<Compilation> get(Pageable pageable, Boolean pinned);

    Compilation getById(Long compId);

    Set<Event> getEvents(Set<Long> eventIds);
}
