package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.dto.State;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConditionsNotMetException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.Status;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.List;

/**
 * Сервис для работы с запросами на участие в событиях
 * Реализует бизнес-логику создания, отмены и получения запросов
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final RequestRepository reqRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestMapper requestMapper;

    /**
     * Создание нового запроса на участие в событии
     *
     * @param userId  идентификатор пользователя, создающего запрос
     * @param eventId идентификатор события, на которое создается запрос
     * @return DTO созданного запроса
     * @throws NotFoundException         если пользователь или событие не найдены
     * @throws ConditionsNotMetException если нарушены условия создания запроса:
     *                                   - запрос уже существует
     *                                   - пользователь является инициатором события
     *                                   - событие не опубликовано
     *                                   - достигнут лимит участников
     */
    @Override
    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        log.info("Создание запроса: userId={}, eventId={}", userId, eventId);

        User user = findUserById(userId);
        Event event = findEventById(eventId);

        //Проверка на повторный запрос (нельзя подать заявку дважды)
        if (reqRepository.existsByRequesterAndEvent(user, event)) {
            log.warn("Запрос уже существует: userId={}, eventId={}", userId, eventId);
            throw new ConditionsNotMetException("Запрос уже существует");
        }

        // Проверка, что инициатор не подает заявку на свое событие
        if (event.getInitiator().equals(user)) {
            log.warn("Инициатор не может создать запрос на свое событие: userId={}, eventId={}", userId, eventId);
            throw new ConditionsNotMetException("Инициатор не может создать запрос на свое событие");
        }

        // Проверка, что событие опубликовано (нельзя подать заявку на черновик)
        if (!event.getState().equals(State.PUBLISHED)) {
            log.warn("Событие не опубликовано: eventId={}, state={}", eventId, event.getState());
            throw new ConditionsNotMetException("Событие должно быть опубликовано");
        }

        // Проверка лимита участников
        int confirmedCount = getConfirmedCount(eventId);
        if (event.getPartLimit() > 0 && event.getPartLimit() <= confirmedCount) {
            log.debug("Достигнут лимит участников: eventId={}, limit={}, confirmed={}",
                    eventId, event.getPartLimit(), confirmedCount);
            throw new ConditionsNotMetException("Достигнут лимит участников");
        }

        Request request = requestMapper.toRequest(user, event);

        //  Автоматическое подтверждение если не требуется модерация или лимит = 0
        //    Если лимит = 0 - безлимитное участие, подтверждаем сразу
        //    Если requestModeration = false - модерация не требуется, подтверждаем сразу
        if (!event.isRequestModeration() || event.getPartLimit() == 0) {
            log.debug("Модерация не требуется, статус CONFIRMED: request для eventId={}", eventId);
            request.setStatus(Status.CONFIRMED);
        }

        Request saved = reqRepository.save(request);
        log.info("Запрос создан: id={}, userId={}, eventId={}", saved.getId(), userId, eventId);

        return requestMapper.toParticipantRequestDto(saved);
    }

    /**
     * Получение всех запросов пользователя
     */
    @Override
    public List<ParticipationRequestDto> findAll(Long userId) {
        log.info("Получение всех запросов пользователя: userId={}", userId);

        User user = findUserById(userId);

        List<Request> requests = reqRepository.findAllByRequester(user);

        log.info("Найдено запросов: {} для userId={}", requests.size(), userId);

        return requestMapper.toParticipantRequestDto(requests);
    }

    /**
     * Отмена запроса на участие
     *
     * @param userId    идентификатор пользователя, отменяющего запрос
     * @param requestId идентификатор отменяемого запроса
     * @return DTO отмененного запроса
     */
    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена запроса: userId={}, requestId={}", userId, requestId);

        User requester = findUserById(userId);
        Request request = findRequestById(requestId);

        //Проверяем, что пользователь действительно создатель запроса
        //    (нельзя отменить чужой запрос)
        if (!request.getRequester().equals(requester)) {
            log.warn("Пользователь не является создателем запроса: userId={}, requestId={}", userId, requestId);
            throw new ConditionsNotMetException(
                    String.format("Пользователь %d не является создателем запроса %d", userId, requestId)
            );
        }

        request.setStatus(Status.CANCELED);

        Request saved = reqRepository.save(request);

        log.info("Запрос отменен: userId={}, requestId={}", userId, requestId);

        return requestMapper.toParticipantRequestDto(saved);
    }

    /**
     * Поиск запроса по ID с проверкой существования
     */
    private Request findRequestById(Long id) {
        return reqRepository.findById(id).orElseThrow(() -> {
            log.warn("Запрос не найден: id={}", id);
            return new NotFoundException("Запрос с id=" + id + " не найден");
        });
    }

    /**
     * Получение количества подтвержденных запросов на событие
     */
    private int getConfirmedCount(Long eventId) {
        return reqRepository.confirmedCount(eventId);
    }

    /**
     * Поиск пользователя по ID с проверкой существования
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> {
            log.warn("Пользователь не найден: id={}", userId);
            return new NotFoundException("Пользователь с id=" + userId + " не найден");
        });
    }

    /**
     * Поиск события по ID с проверкой существования
     */
    private Event findEventById(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> {
            log.warn("Событие не найдено: id={}", id);
            return new NotFoundException("Событие с id=" + id + " не найдено");
        });
    }
}