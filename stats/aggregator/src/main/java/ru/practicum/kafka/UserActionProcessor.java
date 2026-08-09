package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.AggregatorService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionProcessor {
    private final EventSimilarityProducer producer;
    private final AggregatorService service;

    @KafkaListener(
            topics = "${topic.user-actions}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(UserActionAvro action) {
        log.debug("Consumed user action: userId={}, eventId={}", action.getUserId(), action.getEventId());
        List<EventSimilarityAvro> similarities = service.processSimilarities(action);
        similarities.forEach(producer::send);
    }
}
