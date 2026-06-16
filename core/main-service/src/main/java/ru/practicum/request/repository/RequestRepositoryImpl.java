package ru.practicum.request.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;
import ru.practicum.event.model.Event;
import ru.practicum.request.model.QRequest;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.Status;
import ru.practicum.user.model.User;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class RequestRepositoryImpl extends QuerydslRepositorySupport
        implements RequestRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QRequest request = QRequest.request;

    public RequestRepositoryImpl(JPAQueryFactory queryFactory) {
        super(Request.class);
        this.queryFactory = queryFactory;
    }

    @Override
    public int confirmedCount(Long eventId) {
        Long count = queryFactory
                .select(request.count())
                .from(request)
                .where(request.event.id.eq(eventId)
                        .and(request.status.eq(Status.CONFIRMED)))
                .fetchFirst();
        return count != null ? count.intValue() : 0;
    }

    @Override
    public Map<Long, Long> confirmedCount(List<Long> eventIds) {
        List<com.querydsl.core.Tuple> results = queryFactory
                .select(request.event.id, request.count())
                .from(request)
                .where(request.event.id.in(eventIds)
                        .and(request.status.eq(Status.CONFIRMED)))
                .groupBy(request.event.id)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(request.event.id),
                        tuple -> tuple.get(request.count()),
                        (a, b) -> a + b
                ));
    }

    @Override
    public List<Request> findAllByEventId(Long eventId) {
        return queryFactory
                .selectFrom(request)
                .where(request.event.id.eq(eventId))
                .fetch();
    }

    @Override
    public List<Request> findRequestsByIds(List<Long> ids) {
        return queryFactory
                .selectFrom(request)
                .where(request.id.in(ids))
                .orderBy(request.created.asc())
                .fetch();
    }

    @Override
    public List<Request> findAllByEvent(Event event) {
        return queryFactory
                .selectFrom(request)
                .where(request.event.eq(event))
                .fetch();
    }

    @Override
    public List<Request> findAllByRequester(User requester) {
        return queryFactory
                .selectFrom(request)
                .where(request.requester.eq(requester))
                .fetch();
    }

    @Override
    public boolean existsByRequesterAndEvent(User user, Event event) {
        return queryFactory
                .selectFrom(request)
                .where(request.requester.eq(user)
                        .and(request.event.eq(event)))
                .fetchFirst() != null;
    }

    @Override
    public List<Request> findRequestsByStatusAndEvent(Status status, Long eventId) {
        return queryFactory
                .selectFrom(request)
                .where(request.status.eq(status)
                        .and(request.event.id.eq(eventId)))
                .fetch();
    }
}