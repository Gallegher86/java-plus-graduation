package ru.practicum.event.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.category.model.Category;
import ru.practicum.comment.dto.CommentEventDto;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.common.EntityFinder;
import ru.practicum.dto.request.ViewStatsParamDto;
import ru.practicum.dto.response.ViewStatsDto;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.QEvent;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.Status;
import ru.practicum.request.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.practicum.event.model.QEvent.event;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;
    private final EntityFinder entityFinder;
    private final CommentRepository commentRepository;

    // PUBLIC

    @Override
    public List<EventShortDto> getPublicEvents(PublicEventFilterParams params, Pageable pageable) {
        log.info("EventService: поиск событий для public, params={}, pageable={}", params, pageable);

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

        var page = eventRepository.findAll(predicate, pageableToUse);
        List<Event> events = page.getContent();

        // Получаем просмотры пачкой, одним запросом к stats-service
        Map<Long, Integer> viewsMap = fetchViews(events);

        Map<Long, Long> confirmedMap = fetchConfirmedCounts(events);

        List<EventShortDto> result = new ArrayList<>();

        for (Event e : events) {
            long confirmed = confirmedMap.getOrDefault(e.getId(), 0L);
            boolean available = e.getPartLimit() == 0 || confirmed < e.getPartLimit();

            if (Boolean.TRUE.equals(params.getOnlyAvailable()) && !available) {
                continue;
            }

            int views = viewsMap.getOrDefault(e.getId(), 0);
            result.add(eventMapper.toEventShortDto(e, (int) confirmed, views));
        }

        // сортировка по просмотрам
        if (params.getSort() == EventSort.VIEWS) {
            result.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        }

        return result;
    }

    @Override
    public EventFullDto getPublicEvent(Long eventId) {
        log.info("EventService: получение публичного события id={}", eventId);
        Event event = entityFinder.getEventOrThrow(eventId);

        if (event.getState() != State.PUBLISHED) {
            // по ТЗ: для не опубликованного события тоже 404
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        long confirmed = requestRepository.confirmedCount(event.getId());
        int views = fetchViewsForSingleEvent(event);
        List<CommentEventDto> comments =
                commentRepository.findPublishedByEventIds(List.of(event.getId()));

        return eventMapper.toEventFullDto(event, (int) confirmed, views, comments);
    }

    // ADMIN

    @Override
    public List<EventFullDto> getAdminEvents(AdminEventFilterParams params, Pageable pageable) {
        log.info("EventService: поиск событий для admin, params={}, pageable={}", params, pageable);

        BooleanExpression predicate = buildAdminPredicate(params);

        var page = eventRepository.findAll(predicate, pageable);
        List<Event> events = page.getContent();

        //просмотры и количество подтвержденных заявок
        Map<Long, Integer> viewsMap = fetchViews(events);
        Map<Long, Long> confirmedMap = fetchConfirmedCounts(events);
        Map<Long, List<CommentEventDto>> commentsMap = fetchComments(events);

        List<EventFullDto> result = new ArrayList<>();
        for (Event e : events) {
            long confirmed = confirmedMap.getOrDefault(e.getId(), 0L);
            int views = viewsMap.getOrDefault(e.getId(), 0);
            List<CommentEventDto> comments = commentsMap.getOrDefault(e.getId(), Collections.emptyList());
            result.add(eventMapper.toEventFullDto(e, (int) confirmed, views, comments));
        }
        return result;
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        log.info("EventService: админ обновляет event id={}, body={}", eventId, request);

        Event event = entityFinder.getEventOrThrow(eventId);

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
            Category category = entityFinder.getCategoryOrThrow(request.getCategory().longValue());
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
                if (event.getState() != State.PENDING) {
                    throw new ConflictException("Невозможно опубликовать событие в состоянии: " + event.getState());
                }
                event.setState(State.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (request.getStateAction() == StateActionAdmin.REJECT_EVENT) {
                if (event.getState() == State.PUBLISHED) {
                    throw new ConflictException("Невозможно отклонить уже опубликованное событие");
                }
                event.setState(State.CANCELED);
            }
        }

        long confirmed = requestRepository.confirmedCount(event.getId());
        int views = fetchViewsForSingleEvent(event);
        return eventMapper.toEventFullDto(event, (int) confirmed, views, List.of());
    }

    // PRIVATE (user)

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("EventService: user {} создает event {}", userId, newEventDto);

        var initiator = entityFinder.getUserOrThrow(userId);
        var category = entityFinder.getCategoryOrThrow(newEventDto.getCategory().longValue());

        //проверка даты события при создании
        LocalDateTime eventDate = newEventDto.getEventDate();
        LocalDateTime now = LocalDateTime.now();
        if (eventDate.isBefore(now.plusHours(2))) {
            throw new BadRequestException("Дата мероприятия должна быть не ранее чем через 2 часа от текущего момента");
        }

        Event event = eventMapper.toEvent(newEventDto);
        event.setInitiator(initiator);
        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(State.PENDING);
        event.setPublishedOn(null);

        Event saved = eventRepository.save(event);
        // Новое событие ещё никто не смотрел
        return eventMapper.toEventFullDto(saved, 0, 0, List.of());
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {
        log.info("EventService: получаем события для user {}, pageable={}", userId, pageable);

        // фильтрация по инициатору через QueryDSL
        BooleanExpression predicate = event.initiator.id.eq(userId);
        var page = eventRepository.findAll(predicate, pageable);
        List<Event> events = page.getContent();

        //один запрос за просмотрами и один за подтвержденными заявками
        Map<Long, Integer> viewsMap = fetchViews(events);
        Map<Long, Long> confirmedMap = fetchConfirmedCounts(events);

        List<EventShortDto> result = new ArrayList<>();
        for (Event e : events) {
            long confirmed = confirmedMap.getOrDefault(e.getId(), 0L);
            int views = viewsMap.getOrDefault(e.getId(), 0);
            result.add(eventMapper.toEventShortDto(e, (int) confirmed, views));
        }
        return result;
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        log.info("EventService: получаем event {} для user {}", eventId, userId);
        Event event = entityFinder.getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " для пользователя " + userId + " не найдено");
        }

        long confirmed = requestRepository.confirmedCount(event.getId());
        int views = fetchViewsForSingleEvent(event);
        List<CommentEventDto> comments =
                commentRepository.findPublishedByEventIds(List.of(event.getId()));
        return eventMapper.toEventFullDto(event, (int) confirmed, views, comments);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("EventService: user {} обновляет event {}, body={}", userId, eventId, request);

        Event event = entityFinder.getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " для пользователя " + userId + " не найдено");
        }

        if (event.getState() == State.PUBLISHED) {
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
            Category category = entityFinder.getCategoryOrThrow(request.getCategory().longValue());
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
                event.setState(State.PENDING);
            } else if (request.getStateAction() == StateActionUser.CANCEL_REVIEW) {
                event.setState(State.CANCELED);
            }
        }

        long confirmed = requestRepository.confirmedCount(event.getId());
        int views = fetchViewsForSingleEvent(event);
        return eventMapper.toEventFullDto(event, (int) confirmed, views, List.of());
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("EventService: получены запросы на event {} user {}", eventId, userId);

        Event event = entityFinder.getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " для пользователя " + userId + " не найдено");
        }

        List<Request> requests = requestRepository.findAllByEventId(eventId);
        List<ParticipationRequestDto> result = new ArrayList<>();
        for (Request r : requests) {
            result.add(requestMapper.toParticipantRequestDto(r));
        }
        return result;
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequestsStatus(Long userId,
                                                                    Long eventId,
                                                                    EventRequestStatusUpdateRequest req) {
        log.info("EventService: обновить статус запросов для event {} of user {}, body={}",
                eventId, userId, req);

        // тут проверяем, что пришёл только CONFIRMED или REJECTED
        if (req.getStatus() != Status.CONFIRMED && req.getStatus() != Status.REJECTED) {
            throw new ConflictException("Статус должен быть CONFIRMED или REJECTED");
        }

        Event event = entityFinder.getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " для пользователя " + userId + " не найдено");
        }

        List<Request> requests = requestRepository.findRequestsByIds(req.getRequestIds());

        // проверка лимита участников перед подтверждением
        int limit = event.getPartLimit();
        if (req.getStatus() == Status.CONFIRMED && limit != 0) {
            long confirmedBefore = requestRepository.confirmedCount(eventId);

            //сколько заявок из этого списка мы действительно переведем в CONFIRMED (сейчас в PENDING)
            long toConfirm = requests.stream()
                    .filter(r -> r.getStatus() == Status.PENDING)
                    .count();

            //если уже подтвержденные + новые подтверждения превысят лимит - бросаем 409
            if (confirmedBefore + toConfirm > limit) {
                throw new ConflictException("Превышен лимит участников события");
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (Request r : requests) {
            if (r.getStatus() != Status.PENDING) {
                throw new ConflictException("Изменить можно только заявки в статусе PENDING");
            }

            if (req.getStatus() == Status.CONFIRMED) {
                r.setStatus(Status.CONFIRMED);
                confirmed.add(requestMapper.toParticipantRequestDto(r));
            } else {
                r.setStatus(Status.REJECTED);
                rejected.add(requestMapper.toParticipantRequestDto(r));
            }
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }

    // HELPERS: предикаты
    private BooleanExpression buildPublicPredicate(PublicEventFilterParams params) {
        BooleanExpression predicate = event.state.eq(State.PUBLISHED);

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
            predicate = predicate.and(event.initiator.id.in(params.getUsers()));
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

    //  HELPERS: просмотры
    //Получаем просмотры для списка событий одним запросом к stats-service.
    private Map<Long, Integer> fetchViews(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }

        // Собираем URI формата /events/{id}, именно их мы логируем в контроллерах
        Map<Long, String> idToUri = events.stream()
                .filter(e -> e.getId() != null)
                .collect(Collectors.toMap(
                        Event::getId,
                        e -> "/events/" + e.getId()
                ));

        List<String> uris = new ArrayList<>(idToUri.values());

        // Определяем минимальную точку старта по createdOn,
        // если вдруг все null — берем "год назад" как безопасный диапазон
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(now.minusYears(1));

        ViewStatsParamDto params = ViewStatsParamDto.builder()
                .start(start)
                .end(now)
                .uris(uris)
                .unique(true) // уникальные просмотры по IP
                .build();

        List<ViewStatsDto> stats;
        try {
            stats = statsClient.get(params);
        } catch (Exception ex) {
            log.warn("Не удалось получить статистику просмотров, возвращаю 0 для всех событий: {}", ex.getMessage());
            return idToUri.keySet().stream()
                    .collect(Collectors.toMap(id -> id, id -> 0));
        }

        // stats приходят с полями app, uri, hits
        Map<String, Integer> uriToHits = stats.stream()
                .collect(Collectors.toMap(
                        ViewStatsDto::getUri,
                        v -> v.getHits().intValue(),
                        Integer::sum
                ));

        Map<Long, Integer> result = new HashMap<>();
        for (Map.Entry<Long, String> entry : idToUri.entrySet()) {
            Long id = entry.getKey();
            String uri = entry.getValue();
            int hits = uriToHits.getOrDefault(uri, 0);
            result.put(id, hits);
        }

        return result;
    }

    private int fetchViewsForSingleEvent(Event event) {
        if (event == null || event.getId() == null) {
            return 0;
        }
        Map<Long, Integer> map = fetchViews(List.of(event));
        return map.getOrDefault(event.getId(), 0);
    }

    //возвращает количество подтвержденных заявок для списка событий
    private Map<Long, Long> fetchConfirmedCounts(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> ids = events.stream()
                .map(Event::getId)
                .filter(Objects::nonNull)
                .toList();

        return requestRepository.confirmedCount(ids);
    }

    private Map<Long, List<CommentEventDto>> fetchComments(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> ids = events.stream()
                .map(Event::getId)
                .filter(Objects::nonNull)
                .toList();

        List<CommentEventDto> comments = commentRepository.findPublishedByEventIds(ids);

        return comments.stream().collect(Collectors.groupingBy(CommentEventDto::getEventId));
    }
}