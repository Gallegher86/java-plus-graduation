package ru.practicum.event.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.StatsClient;
import ru.practicum.dto.comment.CommentEventDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.dto.request.ViewStatsParamDto;
import ru.practicum.dto.response.ViewStatsDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.ConflictException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventFacadeImpl implements EventFacade {
    private final EventService eventService;
    private final EventMapper eventMapper;
    private final UserClientFacade userClient;
    private final StatsClient statsClient;
    private final RequestClientFacade requestClient;
    private final CommentClientFacade commentClient;

    @Override
    public List<EventShortDto> getPublicEvents(PublicEventFilterParams params, Pageable pageable) {
        log.info("EventService: поиск событий для public, params={}, pageable={}", params, pageable);
        List<Event> events = eventService.getPublicEvents(params, pageable);

        if (events.isEmpty()) {
            return List.of();
        }

        Map<Long, UserShortDto> users = fetchUsers(events);
        Map<Long, Integer> views = fetchViews(events);
        Map<Long, Integer> confirmedRequests = fetchConfirmedRequests(events);

        List<EventShortDto> result = toShortDtos(events, users, views, confirmedRequests);
        log.info("EventService: найдено {} событий.", result.size());
        return result;
    }

    @Override
    public EventFullDto getPublicEvent(Long eventId) {
        log.info("EventService: получение публичного события id={}", eventId);
        Event event = eventService.getPublicEvent(eventId);

        UserShortDto initiator = userClient.getUserShort(event.getInitiatorId());
        int confirmedRequests = requestClient.getConfirmedCountForEvent(eventId);
        int views = fetchViewsForSingleEvent(event);
        List<CommentEventDto> comments = commentClient.getCommentsForEvent(eventId);

        EventFullDto result = eventMapper.toEventFullDto(event, initiator, confirmedRequests, views, comments);
        log.info("EventService: событие id={} получено.", result.getId());
        return result;
    }

    @Override
    public List<EventFullDto> getAdminEvents(AdminEventFilterParams params, Pageable pageable) {
        log.info("EventService: поиск событий для admin, params={}, pageable={}", params, pageable);
        List<Event> events = eventService.getAdminEvents(params, pageable);

        if (events.isEmpty()) {
            return List.of();
        }

        Map<Long, UserShortDto> users = fetchUsers(events);
        Map<Long, Integer> views = fetchViews(events);
        Map<Long, Integer> confirmedRequests = fetchConfirmedRequests(events);
        Map<Long, List<CommentEventDto>> comments = fetchComments(events);

        List<EventFullDto> result = toFullDtos(events, users, views, confirmedRequests, comments);
        log.info("EventService: найдено {} событий.", result.size());
        return result;
    }

    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        log.info("EventService: админ обновляет event id={}, body={}", eventId, request);
        Event event = eventService.updateEventByAdmin(eventId, request);

        UserShortDto initiator = userClient.getUserShort(event.getInitiatorId());

        int confirmedRequests = requestClient.getConfirmedCountForEvent(eventId);
        int views = fetchViewsForSingleEvent(event);
        List<CommentEventDto> comments = commentClient.getCommentsForEvent(eventId);

        EventFullDto result = eventMapper.toEventFullDto(event, initiator, confirmedRequests, views, comments);
        log.info("EventService: событие id={} обновлено.", result.getId());
        return result;
    }

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("EventService: user {} создает event {}", userId, newEventDto);
        UserShortDto initiator = userClient.getUserShort(userId);

        Event result = eventService.createEvent(userId, newEventDto);
        log.info("EventService: event c ID {} создан.", result.getId());
        return eventMapper.toEventFullDto(result, initiator, 0, 0, List.of());
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {
        log.info("EventService: получаем события для user {}, pageable={}", userId, pageable);
        List<Event> events = eventService.getUserEvents(userId, pageable);

        if (events.isEmpty()) {
            return List.of();
        }

        Map<Long, UserShortDto> users = fetchUsers(events);
        Map<Long, Integer> views = fetchViews(events);
        Map<Long, Integer> confirmedRequests = fetchConfirmedRequests(events);

        List<EventShortDto> result = toShortDtos(events, users, views, confirmedRequests);
        log.info("EventService: для пользователя {} найдено {} событий.", userId, result.size());
        return result;
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        log.info("EventService: получаем event {} для user {}", eventId, userId);
        Event event = eventService.getUserEvent(userId, eventId);

        UserShortDto initiator = userClient.getUserShort(event.getInitiatorId());
        int confirmedRequests = requestClient.getConfirmedCountForEvent(eventId);
        int views = fetchViewsForSingleEvent(event);
        List<CommentEventDto> comments = commentClient.getCommentsForEvent(eventId);

        EventFullDto result = eventMapper.toEventFullDto(event, initiator, confirmedRequests, views, comments);
        log.info("EventService: event {} для user {} найден.", result.getId(), result.getInitiator().getId());
        return result;
    }

    @Override
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("EventService: user {} обновляет event {}, body={}", userId, eventId, request);
        Event event = eventService.updateEventByUser(userId, eventId, request);

        UserShortDto initiator = userClient.getUserShort(event.getInitiatorId());

        int confirmedRequests = requestClient.getConfirmedCountForEvent(eventId);
        int views = fetchViewsForSingleEvent(event);
        List<CommentEventDto> comments = commentClient.getCommentsForEvent(eventId);

        EventFullDto result = eventMapper.toEventFullDto(event, initiator, confirmedRequests, views, comments);
        log.info("EventService: event {} для user {} обновлен.", result.getId(), result.getInitiator().getId());
        return result;
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("EventService: получение запросов на event {} user {}", eventId, userId);
        eventService.getEventForRequestsOrThrow(userId, eventId);
        List<ParticipationRequestDto> result = requestClient.getEventRequests(eventId);
        log.info("EventService: получены запросы на event {} user {}, количество {}", eventId, userId, result.size());
        return result;
    }

    @Override
    public EventRequestStatusUpdateResult updateEventRequestsStatus(Long userId,
                                                                    Long eventId,
                                                                    EventRequestStatusUpdateRequest req) {
        log.info("EventService: обновить статус запросов для event {} of user {}, body={}",
                eventId, userId, req);

        // тут проверяем, что пришёл только CONFIRMED или REJECTED
        if (req.getStatus() != RequestStatus.CONFIRMED && req.getStatus() != RequestStatus.REJECTED) {
            throw new ConflictException("Статус должен быть CONFIRMED или REJECTED");
        }

        Event event = eventService.getEventForRequestsOrThrow(userId, eventId);

        List<ParticipationRequestDto> requests = requestClient.getRequestsByIds(req.getRequestIds());

        List<Long> foreignRequestIds = requests.stream()
                .filter(r -> !eventId.equals(r.getEvent()))
                .map(ParticipationRequestDto::getId)
                .toList();

        if (!foreignRequestIds.isEmpty()) {
            throw new ConflictException("Заявки " + foreignRequestIds +
                            " не относятся к событию с id = " + eventId
            );
        }

        // проверка лимита участников перед подтверждением
        int limit = event.getPartLimit();
        if (req.getStatus() == RequestStatus.CONFIRMED && limit != 0) {
            long confirmedBefore = requestClient.getConfirmedCountForEvent(eventId);

            //сколько заявок из этого списка мы действительно переведем в CONFIRMED (сейчас в PENDING)
            long toConfirm = requests.stream()
                    .filter(r -> r.getStatus() == RequestStatus.PENDING)
                    .count();

            //если уже подтвержденные + новые подтверждения превысят лимит - бросаем 409
            if (confirmedBefore + toConfirm > limit) {
                throw new ConflictException("Превышен лимит участников события");
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (ParticipationRequestDto r : requests) {
            if (r.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Изменить можно только заявки в статусе PENDING");
            }

            if (req.getStatus() == RequestStatus.CONFIRMED) {
                r.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(r);
            } else {
                r.setStatus(RequestStatus.REJECTED);
                rejected.add(r);
            }
        }

        requestClient.updateRequestsStatus(eventId, req);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }

    @Override
    public EventInternalDto getEvent(Long eventId) {
        return eventService.getEvent(eventId);
    }

    @Override
    public List<EventInternalDto> getEvents(List<Long> ids) {
        return eventService.getEvents(ids);
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

    private Map<Long, UserShortDto> fetchUsers(List<Event> events) {
        List<Long> ids = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();

        return userClient.getUsersShort(ids)
                .stream()
                .collect(Collectors.toMap(
                        UserShortDto::getId,
                        Function.identity()
                ));
    }

    private Map<Long, Integer> fetchConfirmedRequests(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return requestClient.getConfirmedCountForEvents(eventIds);
    }

    private Map<Long, List<CommentEventDto>> fetchComments(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return commentClient.getCommentsForEvents(eventIds);
    }

    private List<EventShortDto> toShortDtos(
            List<Event> events,
            Map<Long, UserShortDto> users,
            Map<Long, Integer> views,
            Map<Long, Integer> confirmedRequests
    ) {
        List<EventShortDto> result = new ArrayList<>(events.size());

        for (Event e : events) {
            Long eventId = e.getId();
            Long initiatorId = e.getInitiatorId();

            UserShortDto initiator = users.get(initiatorId);
            int viewCount = views.getOrDefault(eventId, 0);
            int confirmed = confirmedRequests.getOrDefault(eventId, 0);

            EventShortDto dto = eventMapper.toEventShortDto(e, initiator, confirmed, viewCount);

            result.add(dto);
        }

        return result;
    }

    private List<EventFullDto> toFullDtos(
            List<Event> events,
            Map<Long, UserShortDto> users,
            Map<Long, Integer> views,
            Map<Long, Integer> confirmedRequests,
            Map<Long, List<CommentEventDto>> comments
    ) {
        List<EventFullDto> result = new ArrayList<>(events.size());

        for (Event e : events) {
            Long eventId = e.getId();
            Long initiatorId = e.getInitiatorId();

            UserShortDto initiator = users.get(initiatorId);
            int viewCount = views.getOrDefault(eventId, 0);
            int confirmed = confirmedRequests.getOrDefault(eventId, 0);
            List<CommentEventDto> eventComments =
                    comments.getOrDefault(eventId, List.of());

            EventFullDto dto = eventMapper.toEventFullDto(e, initiator, confirmed, viewCount, eventComments);

            result.add(dto);
        }

        return result;
    }
}
