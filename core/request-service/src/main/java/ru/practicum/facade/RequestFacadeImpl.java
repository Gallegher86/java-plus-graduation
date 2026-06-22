package ru.practicum.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.event.EventInternalDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.exception.ConditionsNotMetException;
import ru.practicum.model.Request;
import ru.practicum.service.RequestService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestFacadeImpl implements RequestFacade {
    private final UserClient userClient;
    private final EventClient eventClient;
    private final RequestService requestService;

    @Override
    public ParticipationRequestDto create(Long userId, Long eventId) {
        log.info("Создание запроса: userId={}, eventId={}", userId, eventId);
        userClient.checkUser(userId);

        EventInternalDto event = eventClient.getEvent(eventId);

        validateRequest(userId, event);

        Request request = Request.builder()
                .eventId(eventId)
                .requesterId(userId)
                .status(RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .build();

        if (!event.isRequestModeration() || event.getParticipantLimit() == 0) {
            log.debug("Модерация не требуется, статус CONFIRMED: request для eventId={}", eventId);
            request.setStatus(RequestStatus.CONFIRMED);
        }

        return requestService.create(request);
    }

    @Override
    public List<ParticipationRequestDto> findAll(Long userId) {
        log.info("Получение всех запросов пользователя: userId={}", userId);
        userClient.checkUser(userId);
        return requestService.findAll(userId);
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена запроса: userId={}, requestId={}", userId, requestId);
        userClient.checkUser(userId);
        return requestService.cancelRequest(userId, requestId);
    }

    private void validateRequest(Long userId, EventInternalDto event) {
        Long eventId = event.getId();
        List<String> errors = new ArrayList<>();

        if (requestService.checkRequestExists(userId, eventId)) {
            errors.add("Запрос уже существует.");
        }

        if (event.getInitiatorId().equals(userId)) {
            errors.add("Инициатор не может создать запрос на свое событие.");
        }

        if (!event.getState().equals(EventState.PUBLISHED)) {
            errors.add("Событие должно быть опубликовано.");
        }

        int confirmedCount = requestService.getConfirmedCount(eventId);
        int participantLimit = event.getParticipantLimit();

        if (participantLimit > 0 && participantLimit <= confirmedCount) {
            errors.add("Достигнут лимит участников.");
        }

        if (!errors.isEmpty()) {
            log.warn("Валидация не пройдена: userId={}, eventId={}, errors={}", userId, eventId, errors);
            throw new ConditionsNotMetException("Не удалось создать запрос.", errors);
        }
    }
}
