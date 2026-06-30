package ru.practicum.compilation.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.client.UserClient;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.service.CompilationService;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationFacadeImpl implements CompilationFacade {
    private final CompilationService compilationService;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;
    private final UserClient userClient;

    @Override
    public CompilationDto create(NewCompilationDto dto) {
        log.info("CompilationService: получен запрос на создание компиляции: {}.", dto);
        Set<Event> events = compilationService.getEvents(dto.getEvents());
        Map<Long, UserShortDto> initiatorsMap = getUsers(events);

        Compilation entity = compilationMapper.toCompilation(dto, events);

        CompilationDto result = compilationService.create(entity);

        result.setEvents(setInitiators(events, initiatorsMap));
        log.info("CompilationService: компиляция сохранена: {}", result);
        return result;
    }

    @Override
    public void delete(Long compId) {
        compilationService.delete(compId);
    }

    @Override
    public CompilationDto update(Long compId, UpdateCompilationRequest dto) {
        log.info("CompilationService: получен запрос на обновление компиляции с id {}.", compId);
        Set<Event> events = compilationService.getEvents(dto.getEvents());
        Map<Long, UserShortDto> initiatorsMap = getUsers(events);

        CompilationDto result = compilationService.update(compId, dto);

        result.setEvents(setInitiators(events, initiatorsMap));
        log.info("CompilationService: категория обновлена: {}", result);
        return result;
    }

    @Override
    public List<CompilationDto> get(Pageable pageable, Boolean pinned) {
        log.info("CompilationService: получен запрос на получение списка компиляций.");
        List<Compilation> compilations = compilationService.get(pageable, pinned);

        Set<Event> allEvents = compilations.stream()
                .flatMap(c -> c.getEvents().stream())
                .collect(Collectors.toSet());

        Map<Long, UserShortDto> initiatorsMap = getUsers(allEvents);

        List<CompilationDto> result = new ArrayList<>();

        for (Compilation compilation : compilations) {
            CompilationDto dto = compilationMapper.toCompilationDto(compilation);
            dto.setEvents(setInitiators(compilation.getEvents(), initiatorsMap));
            result.add(dto);
        }

        log.info("CompilationService: выдана страница компиляций размером: {}, начиная с {}.",
                result.size(), pageable.getOffset());
        return result;
    }

    @Override
    public CompilationDto getById(Long compId) {
        log.info("CompilationService: получен запрос на получение компиляции с id: {}.", compId);
        Compilation compilation = compilationService.getById(compId);
        Set<Event> events = compilation.getEvents();
        Map<Long, UserShortDto> initiatorsMap = getUsers(events);

        CompilationDto result = compilationMapper.toCompilationDto(compilation);
        result.setEvents(setInitiators(events, initiatorsMap));
        log.info("CompilationService: компиляция выдана: {}", result);
        return result;
    }

    private Map<Long, UserShortDto> getUsers(Set<Event> events) {
        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();

        return userClient.getUsersShort(initiatorIds)
                .stream()
                .collect(Collectors.toMap(UserShortDto::getId, u -> u));
    }

    private List<EventShortDto> setInitiators(Set<Event> events, Map<Long, UserShortDto> initiatorsMap) {
        return events.stream()
                .map(event -> eventMapper.toEventShortDto(
                        event,
                        initiatorsMap.get(event.getInitiatorId()),
                        0,
                        0
                ))
                .toList();
    }
}
