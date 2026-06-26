package ru.practicum.event.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.dto.event.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.QEvent;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.event.model.QEvent.event;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;

    @Override
    public List<Event> getPublicEvents(PublicEventFilterParams params, Pageable pageable) {
        BooleanExpression predicate = buildPublicPredicate(params);

        //если сортировка по дате - задаем ее сразу в запросе к БД
        Pageable pageableToUse = pageable;
        if (params.getSort() == EventSort.EVENT_DATE) {
            pageableToUse = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.ASC, "eventDate")
            );
        }

        return eventRepository.findAll(predicate, pageableToUse).getContent();
    }

    @Override
    public Event getPublicEvent(Long eventId) {
        Event event = getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            // по ТЗ: для не опубликованного события тоже 404
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        return event;
    }

    @Override
    public List<Event> getAdminEvents(AdminEventFilterParams params, Pageable pageable) {
        BooleanExpression predicate = buildAdminPredicate(params);

        var page = eventRepository.findAll(predicate, pageable);
        return page.getContent();
    }

    @Override
    @Transactional
    public Event updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = getEventOrThrow(eventId);

        // обновляем только ненулевые поля
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getCategory() != null) {
            Category category = getCategoryOrThrow(request.getCategory().longValue());
            event.setCategory(category);
        }
        if (request.getLocation() != null) {
            event.setLocLat(request.getLocation().lat());
            event.setLocLon(request.getLocation().lon());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setPartLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getEventDate() != null) {
            // правило: дата начала не ранее чем за час от текущего момента (публикации)
            LocalDateTime newDate = request.getEventDate();
            LocalDateTime now = LocalDateTime.now();
            if (newDate.isBefore(now.plusHours(1))) {
                throw new BadRequestException("Дата начала события должна быть не ранее чем за час от момента публикации");
            }
            event.setEventDate(newDate);
        }

        // смена статуса админом
        if (request.getStateAction() != null) {
            if (request.getStateAction() == StateActionAdmin.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Невозможно опубликовать событие в состоянии: " + event.getState());
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (request.getStateAction() == StateActionAdmin.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Невозможно отклонить уже опубликованное событие");
                }
                event.setState(EventState.CANCELED);
            }
        }

        return event;
    }

    @Override
    @Transactional
    public Event createEvent(Long userId, NewEventDto newEventDto) {
        log.info("EventService: user {} создает event {}", userId, newEventDto);

        Category category = getCategoryOrThrow(newEventDto.getCategory().longValue());

        //проверка даты события при создании
        LocalDateTime eventDate = newEventDto.getEventDate();
        LocalDateTime now = LocalDateTime.now();

        if (eventDate.isBefore(now.plusHours(2))) {
            throw new BadRequestException("Дата мероприятия должна быть не ранее чем через 2 часа от текущего момента");
        }

        Event event = eventMapper.toEvent(newEventDto);
        event.setInitiatorId(userId);
        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);
        event.setPublishedOn(null);

        return eventRepository.save(event);
    }

    @Override
    public List<Event> getUserEvents(Long userId, Pageable pageable) {
        // фильтрация по инициатору через QueryDSL
        BooleanExpression predicate = event.initiatorId.eq(userId);
        return eventRepository.findAll(predicate, pageable).getContent();
    }

    @Override
    public Event getUserEvent(Long userId, Long eventId) {
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " для пользователя " + userId + " не найдено");
        }

        return event;
    }

    @Override
    @Transactional
    public Event updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " для пользователя " + userId + " не найдено");
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Изменить можно только отложенное или отменённое событие");
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getCategory() != null) {
            Category category = getCategoryOrThrow(request.getCategory().longValue());
            event.setCategory(category);
        }
        if (request.getLocation() != null) {
            event.setLocLat(request.getLocation().lat());
            event.setLocLon(request.getLocation().lon());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setPartLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getEventDate() != null) {
            LocalDateTime newDate = request.getEventDate();
            LocalDateTime now = LocalDateTime.now();
            // здесь оставили 2 часа для пользователя
            if (newDate.isBefore(now.plusHours(2))) {
                throw new BadRequestException("Дата мероприятия должна быть не ранее чем через 2 часа от текущего момента");
            }
            event.setEventDate(newDate);
        }

        if (request.getStateAction() != null) {
            if (request.getStateAction() == StateActionUser.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            } else if (request.getStateAction() == StateActionUser.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            }
        }

        return event;
    }

    @Override
    public Event getEventForRequestsOrThrow(Long userId, Long eventId) {
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " для пользователя " + userId + " не найдено");
        }

        return event;
    }

    // HELPERS: предикаты
    private BooleanExpression buildPublicPredicate(PublicEventFilterParams params) {
        BooleanExpression predicate = event.state.eq(EventState.PUBLISHED);

        if (params.getText() != null && !params.getText().isBlank()) {
            String text = params.getText();
            BooleanExpression textExpr =
                    event.annotation.containsIgnoreCase(text)
                            .or(event.description.containsIgnoreCase(text));
            predicate = predicate.and(textExpr);
        }

        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            predicate = predicate.and(event.category.id.in(params.getCategories()));
        }

        if (params.getPaid() != null) {
            predicate = predicate.and(event.paid.eq(params.getPaid()));
        }

        LocalDateTime start = params.getRangeStart();
        LocalDateTime end = params.getRangeEnd();
        LocalDateTime now = LocalDateTime.now();

        if (start == null && end == null) {
            predicate = predicate.and(event.eventDate.after(now));
        } else {
            if (start != null) {
                predicate = predicate.and(event.eventDate.goe(start));
            }
            if (end != null) {
                if (start != null && end.isBefore(start)) {
                    throw new BadRequestException("Дата конца события не может быть раньше даты начала");
                }
                predicate = predicate.and(event.eventDate.loe(end));
            }
        }

        return predicate;
    }

    private BooleanExpression buildAdminPredicate(AdminEventFilterParams params) {
        BooleanExpression predicate = QEvent.event.isNotNull();

        if (params.getUsers() != null && !params.getUsers().isEmpty()) {
            predicate = predicate.and(event.initiatorId.in(params.getUsers()));
        }

        if (params.getStates() != null && !params.getStates().isEmpty()) {
            predicate = predicate.and(event.state.in(params.getStates()));
        }

        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            predicate = predicate.and(event.category.id.in(params.getCategories()));
        }

        LocalDateTime start = params.getRangeStart();
        LocalDateTime end = params.getRangeEnd();

        if (start != null) {
            predicate = predicate.and(event.eventDate.goe(start));
        }
        if (end != null) {
            if (start != null && end.isBefore(start)) {
                throw new BadRequestException("Дата конца события не может быть раньше даты начала");
            }
            predicate = predicate.and(event.eventDate.loe(end));
        }

        return predicate;
    }

    private Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + id + " не найдена"));
    }

    private Event getEventOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено"));
    }
}
