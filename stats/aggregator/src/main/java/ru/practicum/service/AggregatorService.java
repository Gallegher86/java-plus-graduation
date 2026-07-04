package ru.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AggregatorService {
    private final Map<Long, Double> eventWeightSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();
    private final Map<Long, Map<Long, Double>> userEventWeights = new HashMap<>();

    /**
     * Обновляет коэффициенты сходства мероприятий после нового действия пользователя.
     */
    public List<EventSimilarityAvro> processSimilarities(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();

        double newWeight = getWeight(action.getActionType());
        double oldWeight = getCurrentWeight(eventId, userId);

        if (newWeight <= oldWeight) {
            return List.of();
        }

        updateUserWeight(eventId, userId, oldWeight, newWeight);

        return calculateSimilarities(action, eventId, userId, oldWeight, newWeight);
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

    /**
     * Возвращает текущий максимальный вес пользователя для мероприятия.
     */
    private double getCurrentWeight(long eventId, long userId) {
        return eventUserWeights
                .computeIfAbsent(eventId, id -> new HashMap<>())
                .getOrDefault(userId, 0.0);
    }

    /**
     * Обновляет вес пользователя и суммарный вес мероприятия.
     */
    private void updateUserWeight(long eventId,
                                  long userId,
                                  double oldWeight,
                                  double newWeight) {

        eventUserWeights
                .computeIfAbsent(eventId, id -> new HashMap<>())
                .put(userId, newWeight);

        userEventWeights
                .computeIfAbsent(userId, id -> new HashMap<>())
                .put(eventId, newWeight);

        double delta = newWeight - oldWeight;
        eventWeightSums.merge(eventId, delta, Double::sum);
    }

    /**
     * Пересчитывает сходство текущего мероприятия с остальными.
     */
    private List<EventSimilarityAvro> calculateSimilarities(
            UserActionAvro action,
            long eventId,
            long userId,
            double oldWeight,
            double newWeight) {

        List<EventSimilarityAvro> result = new ArrayList<>();

        Map<Long, Double> userEvents = userEventWeights.get(userId);

        if (userEvents == null) {
            return result;
        }

        for (Map.Entry<Long, Double> entry : userEvents.entrySet()) {

            long otherEventId = entry.getKey();

            if (otherEventId == eventId) {
                continue;
            }

            double otherWeight = entry.getValue();

            Double similarity = updateSimilarity(
                    eventId,
                    otherEventId,
                    oldWeight,
                    newWeight,
                    otherWeight);

            if (similarity == null) {
                continue;
            }

            long eventA = Math.min(eventId, otherEventId);
            long eventB = Math.max(eventId, otherEventId);

            result.add(createEventSimilarity(
                    eventA,
                    eventB,
                    similarity,
                    action.getTimestamp()));
        }

        return result;
    }

    /**
     * Обновляет накопленную сумму минимальных весов и вычисляет коэффициент сходства пары мероприятий.
     */
    private Double updateSimilarity(long eventId,
                                    long otherEventId,
                                    double oldWeight,
                                    double newWeight,
                                    double otherWeight) {

        double oldMin = getMinWeightSum(eventId, otherEventId);

        double newMin = oldMin + (Math.min(newWeight, otherWeight) - Math.min(oldWeight, otherWeight));

        putMinWeightSum(eventId, otherEventId, newMin);

        double eventWeight = eventWeightSums.getOrDefault(eventId, 0.0);
        double otherEventWeight = eventWeightSums.getOrDefault(otherEventId, 0.0);

        if (eventWeight <= 0 || otherEventWeight <= 0) {
            return null;
        }

        return newMin / (Math.sqrt(eventWeight) * Math.sqrt(otherEventWeight));
    }

    /**
     * Создает сообщение с рассчитанным коэффициентом сходства мероприятий.
     */
    private EventSimilarityAvro createEventSimilarity(
            long eventA,
            long eventB,
            double similarity,
            Instant timestamp) {

        return EventSimilarityAvro.newBuilder()
                .setEventA(eventA)
                .setEventB(eventB)
                .setScore(similarity)
                .setTimestamp(timestamp)
                .build();
    }

    /**
     * Сохраняет сумму минимальных весов для пары мероприятий.
     */
    private void putMinWeightSum(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        minWeightsSums
                .computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);
    }

    /**
     * Возвращает накопленную сумму минимальных весов для пары мероприятий.
     */
    private double getMinWeightSum(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return minWeightsSums
                .computeIfAbsent(first, e -> new HashMap<>())
                .getOrDefault(second, 0.0);
    }
}
