package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.constant.ActionWeight;
import ru.practicum.dto.ScoredEvent;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.model.*;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserActionRepository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnalyzerService {
    private final EventSimilarityRepository eventSimilarityRepository;
    private final UserActionRepository userActionRepository;

    private static final int INTERACTIONS_LIM = 20;
    private static final int K_LIM = 5;

    @Transactional
    public void saveEventSimilarity(EventSimilarityAvro similarity) {
        EventSimilarityId id = new EventSimilarityId(similarity.getEventA(), similarity.getEventB());

        EventSimilarity entity = EventSimilarity.builder()
                .id(id)
                .score(similarity.getScore())
                .timestamp(similarity.getTimestamp())
                .build();

        eventSimilarityRepository.save(entity);

        log.debug("EventSimilarity сохранен: eventA={}, eventB={}, score={}",
                entity.getId().getEventA(),
                entity.getId().getEventB(),
                entity.getScore());
    }

    @Transactional
    public void saveUserAction(UserActionAvro action) {
        UserActionId id = new UserActionId(action.getUserId(), action.getEventId());
        double weight = getWeight(action.getActionType());
        Instant timestamp = action.getTimestamp();

        UserAction entity = userActionRepository.findById(id).orElse(null);

        if (entity == null) {
            entity = new UserAction(id, weight, timestamp);
            userActionRepository.save(entity);
            log.debug(
                    "UserAction сохранен: userId={}, eventId={}, weight={}",
                    entity.getId().getUserId(),
                    entity.getId().getEventId(),
                    entity.getWeight());
        } else if (entity.getWeight() < weight) {
            entity.setWeight(weight);
            entity.setTimestamp(timestamp);
            log.debug(
                    "UserAction обновлен: userId={}, eventId={}, weight={}",
                    entity.getId().getUserId(),
                    entity.getId().getEventId(),
                    entity.getWeight());
        }
    }

    /**
     * Формирует список рекомендаций для пользователя на основе:
     * - последних взаимодействий пользователя
     * - похожести событий
     * - предсказанной оценки для каждого кандидата
     */
    public List<ScoredEvent> getRecommendationsForUser(long userId, int maxResults) {
        Set<Long> recentEventIds = new HashSet<>(
                userActionRepository.findRecentEventIdsByUserId(
                        userId,
                        PageRequest.of(0, INTERACTIONS_LIM))
        );

        if (recentEventIds.isEmpty()) {
            return List.of();
        }

        Set<Long> interactedEventIds = new HashSet<>(
                userActionRepository.findAllInteractedEventIdsByUser(userId));

        List<EventSimilarity> candidates = eventSimilarityRepository.findSimilarEvents(
                recentEventIds,
                interactedEventIds,
                PageRequest.of(0, maxResults * 2));

        Map<Long, Double> userWeights = userActionRepository.findAllByUserId(userId)
                .stream()
                .collect(Collectors.toMap(
                        ua -> ua.getId().getEventId(),
                        UserAction::getWeight
                ));

        Set<Long> candidateIds = candidates.stream()
                .map(es -> getCandidateId(es, recentEventIds))
                .collect(Collectors.toSet());

        return candidateIds.stream()
                .map(candidateId -> {
                    double score = predictScore(
                            candidateId,
                            userWeights,
                            interactedEventIds
                    );
                    return new ScoredEvent(candidateId, score);
                })
                .sorted(Comparator.comparingDouble(ScoredEvent::score).reversed())
                .limit(maxResults)
                .toList();
    }

    /**
     * Возвращает наиболее похожие на указанное событие мероприятия,
     * с которыми пользователь еще не взаимодействовал.
     */
    public List<ScoredEvent> getSimilarEvents(long userId,
                                              long eventId,
                                              int maxResults) {

        Set<Long> interactedEventIds = new HashSet<>(
                userActionRepository.findAllInteractedEventIdsByUser(userId));

        return eventSimilarityRepository
                .findByEvent(
                        eventId,
                        interactedEventIds,
                        PageRequest.of(0, maxResults))
                .stream()
                .map(es -> new ScoredEvent(
                        es.getId().getEventA() == eventId
                                ? es.getId().getEventB()
                                : es.getId().getEventA(),
                        es.getScore()
                ))
                .toList();
    }

    /**
     * Возвращает суммарный вес взаимодействий пользователей
     * с указанными событиями (простая метрика популярности).
     */
    public List<ScoredEvent> getInteractionsCount(List<Long> eventIds) {
        return userActionRepository.sumWeightsByEventIds(eventIds);
    }

    /**
     * Определяет id рекомендуемого события из пары похожих событий,
     * исключая уже просмотренные пользователем.
     */
    private long getCandidateId(EventSimilarity similarity,
                                Set<Long> recentEventIds) {

        return recentEventIds.contains(similarity.getId().getEventA())
                ? similarity.getId().getEventB()
                : similarity.getId().getEventA();
    }

    /**
     * Предсказывает "оценку" события на основе:
     * - K ближайших уже просмотренных пользователем событий
     * - их коэффициента похожести
     * - весов пользовательских взаимодействий.
     * Используется взвешенное среднее по сходству.
     */
    private double predictScore(
            long candidateId,
            Map<Long, Double> userWeights,
            Set<Long> interactedEventIds
    ) {
        List<EventSimilarity> nearest = eventSimilarityRepository
                .findNearestViewedEvents(
                        candidateId,
                        interactedEventIds,
                        PageRequest.of(0, K_LIM));

        double weightedSum = 0.0;
        double similaritySum = 0.0;

        for (EventSimilarity es : nearest) {

            long other = es.getId().getEventA() == candidateId
                    ? es.getId().getEventB()
                    : es.getId().getEventA();

            Double userWeight = userWeights.get(other);
            if (userWeight == null) {
                continue;
            }

            double sim = es.getScore();

            weightedSum += sim * userWeight;
            similaritySum += Math.abs(sim);
        }

        return similaritySum == 0.0 ? 0.0 : weightedSum / similaritySum;
    }

    /**
     * Возвращает вес действия пользователя.
     */
    private double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> ActionWeight.ACTION_VIEW_WEIGHT;
            case REGISTER -> ActionWeight.ACTION_REGISTER_WEIGHT;
            case LIKE -> ActionWeight.ACTION_LIKE_WEIGHT;
        };
    }
}
