package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSimilarityProducer {
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    @Value("${topic.event-similarity}")
    private String topic;

    public void send(EventSimilarityAvro similarity) {
        long eventA = similarity.getEventA();
        long eventB = similarity.getEventB();

        String key = Math.min(eventA, eventB) + ":" + Math.max(eventA, eventB);

        kafkaTemplate.send(topic, key, similarity);

        log.debug("Отправлено Сходство: key={}, eventA={}, eventB={}, score={}",
                key,
                eventA,
                eventB,
                similarity.getScore());
    }
}
