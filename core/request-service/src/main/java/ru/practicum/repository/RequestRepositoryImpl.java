package ru.practicum.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.model.QRequest;
import ru.practicum.model.Request;

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
    public int confirmedCountForEvent(Long eventId) {
        Long count = queryFactory
                .select(request.count())
                .from(request)
                .where(
                        request.eventId.eq(eventId),
                        request.status.eq(RequestStatus.CONFIRMED)
                )
                .fetchOne();

        return count != null ? count.intValue() : 0;
    }

    @Override
    public Map<Long, Integer> confirmedCountForEvents(List<Long> eventIds) {
        List<Tuple> results = queryFactory
                .select(request.eventId, request.count())
                .from(request)
                .where(
                        request.eventId.in(eventIds),
                        request.status.eq(RequestStatus.CONFIRMED)
                )
                .groupBy(request.eventId)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        t -> t.get(request.eventId),
                        t -> {
                            Long c = t.get(request.count());
                            return c == null ? 0 : c.intValue();
                        }
                ));
    }

    @Override
    public List<Request> findAllByEventId(Long eventId) {
        return queryFactory
                .selectFrom(request)
                .where(request.eventId.eq(eventId))
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
    public List<Request> findAllByRequesterId(Long requesterId) {
        return queryFactory
                .selectFrom(request)
                .where(request.requesterId.eq(requesterId))
                .fetch();
    }

    @Override
    public boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId) {
        return queryFactory
                .selectOne()
                .from(request)
                .where(
                        request.requesterId.eq(requesterId),
                        request.eventId.eq(eventId)
                )
                .fetchFirst() != null;
    }

    @Override
    public List<Request> findRequestsByStatusAndEventId(RequestStatus status, Long eventId) {
        return queryFactory
                .selectFrom(request)
                .where(
                        request.status.eq(status),
                        request.eventId.eq(eventId)
                )
                .fetch();
    }
}
