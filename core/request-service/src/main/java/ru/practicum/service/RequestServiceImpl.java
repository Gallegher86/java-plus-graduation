package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.exception.ConditionsNotMetException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.repository.RequestRepository;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final RequestRepository repository;
    private final RequestMapper requestMapper;

    @Override
    @Transactional
    public ParticipationRequestDto create(Request request) {
        Request saved = repository.save(request);
        log.info("Запрос создан: id={}, userId={}, eventId={}", saved.getId(), saved.getRequesterId(), saved.getEventId());
        return requestMapper.toParticipantRequestDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> findAll(Long userId) {
        List<Request> result = repository.findAllByRequesterId(userId);
        log.info("Найдено запросов: {} для userId={}", result.size(), userId);
        return result.stream()
                .map(requestMapper::toParticipantRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = repository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с ID " + requestId + " не найден."));

        if (!request.getRequesterId().equals(userId)) {
            throw new ConditionsNotMetException(
                    String.format("Пользователь %d не является создателем запроса %d", userId, requestId));
        }

        request.setStatus(RequestStatus.CANCELED);
        log.info("Запрос отменен: userId={}, requestId={}", userId, requestId);
        return requestMapper.toParticipantRequestDto(request);
    }

    @Override
    public int getConfirmedCount(Long eventId) {
        return (int) repository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    @Override
    public boolean checkRequestExists(Long userId, Long eventId) {
        return repository.existsByEventIdAndRequesterId(eventId, userId);
    }
}
