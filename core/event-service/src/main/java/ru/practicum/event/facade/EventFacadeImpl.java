package ru.practicum.event.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.client.AnalyzerClient;
import ru.practicum.client.CollectorClient;
import ru.practicum.dto.comment.CommentEventDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.service.EventService;
import ru.practicum.ewm.stats.proto.analyzer.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.exception.ConflictException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventFacadeImpl implements EventFacade {
    private final EventService eventService;
    private final EventMapper eventMapper;
    private final UserClientFacade userClient;
    private final RequestClientFacade requestClient;
    private final CommentClientFacade commentClient;
    private final CollectorClient collectorClient;
    private final AnalyzerClient analyzerClient;

    private static final int DEFAULT_RECOMMENDATIONS_SIZE = 10;

    @Override
    public List<EventShortDto> getPublicEvents(PublicEventFilterParams params, Pageable pageable) {
        log.info("EventService: поиск событий для public, params={}, pageable={}", params, pageable);
        List<Event> events = eventService.getPublicEvents(params, pageable);

        if (events.isEmpty()) {
            return List.of();
        }

        Map<Long, UserShortDto> users = fetchUsers(events);
        Map<Long, Double> ratings = fetchRatings(events);
        Map<Long, Integer> confirmedRequests = fetchConfirmedRequests(events);

        List<EventShortDto> result = toShortDtos(events, users, ratings, confirmedRequests);
        log.info("EventService: найдено {} событий.", result.size());
        return result;
    }

    @Override
    public EventFullDto getPublicEvent(Long eventId, Long userId) {
        log.info("EventService: получение публичного события id={}", eventId);
        userClient.checkUser(userId);
        Event event = eventService.getPublicEvent(eventId);

        UserShortDto initiator = userClient.getUserShort(event.getInitiatorId());
        int confirmedRequests = requestClient.getConfirmedCountForEvent(eventId);
        double rating = fetchRating(eventId);
        List<CommentEventDto> comments = commentClient.getCommentsForEvent(eventId);

        EventFullDto result = eventMapper.toEventFullDto(event, initiator, confirmedRequests, rating, comments);
        collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
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
        Map<Long, Double> ratings = fetchRatings(events);
        Map<Long, Integer> confirmedRequests = fetchConfirmedRequests(events);
        Map<Long, List<CommentEventDto>> comments = fetchComments(events);

        List<EventFullDto> result = toFullDtos(events, users, ratings, confirmedRequests, comments);
        log.info("EventService: найдено {} событий для админа.", result.size());
        return result;
    }

    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        log.info("EventService: админ обновляет event id={}, body={}", eventId, request);
        Event event = eventService.updateEventByAdmin(eventId, request);

        UserShortDto initiator = userClient.getUserShort(event.getInitiatorId());

        int confirmedRequests = requestClient.getConfirmedCountForEvent(eventId);
        double rating = fetchRating(eventId);
        List<CommentEventDto> comments = commentClient.getCommentsForEvent(eventId);

        EventFullDto result = eventMapper.toEventFullDto(event, initiator, confirmedRequests, rating, comments);
        log.info("EventService: событие id={} обновлено.", result.getId());
        return result;
    }

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("EventService: user {} создает event {}", userId, newEventDto);
        UserShortDto initiator = userClient.getUserShort(userId);

        Event result = eventService.createEvent(userId, newEventDto);
        log.info("EventService: event c ID {} создан.", result.getId());
        return eventMapper.toEventFullDto(result, initiator, 0, 0.0, List.of());
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {
        log.info("EventService: получаем события для user {}, pageable={}", userId, pageable);
        List<Event> events = eventService.getUserEvents(userId, pageable);

        if (events.isEmpty()) {
            return List.of();
        }

        Map<Long, UserShortDto> users = fetchUsers(events);
        Map<Long, Double> ratings = fetchRatings(events);
        Map<Long, Integer> confirmedRequests = fetchConfirmedRequests(events);

        List<EventShortDto> result = toShortDtos(events, users, ratings, confirmedRequests);
        log.info("EventService: для пользователя {} найдено {} событий.", userId, result.size());
        return result;
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        log.info("EventService: получаем event {} для user {}", eventId, userId);
        Event event = eventService.getUserEvent(userId, eventId);

        UserShortDto initiator = userClient.getUserShort(event.getInitiatorId());
        int confirmedRequests = requestClient.getConfirmedCountForEvent(eventId);
        double rating = fetchRating(eventId);
        List<CommentEventDto> comments = commentClient.getCommentsForEvent(eventId);

        EventFullDto result = eventMapper.toEventFullDto(event, initiator, confirmedRequests, rating, comments);
        log.info("EventService: event {} для user {} найден.", result.getId(), result.getInitiator().getId());
        return result;
    }

    @Override
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("EventService: user {} обновляет event {}, body={}", userId, eventId, request);
        Event event = eventService.updateEventByUser(userId, eventId, request);

        UserShortDto initiator = userClient.getUserShort(event.getInitiatorId());

        int confirmedRequests = requestClient.getConfirmedCountForEvent(eventId);
        double rating = fetchRating(eventId);
        List<CommentEventDto> comments = commentClient.getCommentsForEvent(eventId);

        EventFullDto result = eventMapper.toEventFullDto(event, initiator, confirmedRequests, rating, comments);
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

    @Override
    public List<EventShortDto> getRecommendations(Long userId) {
        userClient.checkUser(userId);
        List<RecommendedEventProto> recommendations = analyzerClient
                .getRecommendationsForUser(userId, DEFAULT_RECOMMENDATIONS_SIZE);

        if (recommendations.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = recommendations.stream().map(RecommendedEventProto::getEventId).toList();
        List<Event> events = eventService.getEventsForRecommendations(eventIds);

        Map<Long, Integer> order = IntStream.range(0, eventIds.size())
                .boxed()
                .collect(Collectors.toMap(eventIds::get, Function.identity()));

        events.sort(Comparator.comparingInt(event -> order.get(event.getId())));

        Map<Long, UserShortDto> users = fetchUsers(events);
        Map<Long, Double> ratings = fetchRatings(events);
        Map<Long, Integer> confirmedRequests = fetchConfirmedRequests(events);

        List<EventShortDto> result = toShortDtos(events, users, ratings, confirmedRequests);
        log.info("EventService: найдено {} событий для рекомендаций.", result.size());
        return result;
    }

    @Override
    public void likeEvent(Long userId, Long eventId) {
        userClient.checkUser(userId);
        eventService.getPublicEvent(eventId);
        collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
        log.debug("EventService: пользователь {} поставил лайк событию {}.", userId, eventId);
    }

    //  HELPERS: просмотры
    private Double fetchRating(Long eventId) {
        return analyzerClient.getInteractionsCount(List.of(eventId))
                .stream()
                .findFirst()
                .map(RecommendedEventProto::getScore)
                .orElse(0.0);
    }

    private Map<Long, Double> fetchRatings(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> ids = events.stream()
                .map(Event::getId)
                .toList();

        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(ids)
                .stream()
                .collect(Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore
                ));

        ids.forEach(id -> ratings.putIfAbsent(id, 0.0));

        return ratings;
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
            Map<Long, Double> ratings,
            Map<Long, Integer> confirmedRequests
    ) {
        List<EventShortDto> result = new ArrayList<>(events.size());

        for (Event e : events) {
            Long eventId = e.getId();
            Long initiatorId = e.getInitiatorId();

            UserShortDto initiator = users.get(initiatorId);
            double rating = ratings.getOrDefault(eventId, 0.0);
            int confirmed = confirmedRequests.getOrDefault(eventId, 0);

            EventShortDto dto = eventMapper.toEventShortDto(e, initiator, confirmed, rating);

            result.add(dto);
        }

        return result;
    }

    private List<EventFullDto> toFullDtos(
            List<Event> events,
            Map<Long, UserShortDto> users,
            Map<Long, Double> ratings,
            Map<Long, Integer> confirmedRequests,
            Map<Long, List<CommentEventDto>> comments
    ) {
        List<EventFullDto> result = new ArrayList<>(events.size());

        for (Event e : events) {
            Long eventId = e.getId();
            Long initiatorId = e.getInitiatorId();

            UserShortDto initiator = users.get(initiatorId);
            double rating = ratings.getOrDefault(eventId, 0.0);
            int confirmed = confirmedRequests.getOrDefault(eventId, 0);
            List<CommentEventDto> eventComments =
                    comments.getOrDefault(eventId, List.of());

            EventFullDto dto = eventMapper.toEventFullDto(e, initiator, confirmed, rating, eventComments);

            result.add(dto);
        }

        return result;
    }
}
