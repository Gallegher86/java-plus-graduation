package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.service.AnalyzerService;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityConsumer {
    private final AnalyzerService analyzerService;

    @KafkaListener(
            topics = "${topic.event-similarity}",
            containerFactory = "eventSimilarityKafkaListenerContainerFactory"
    )
    public void consume(EventSimilarityAvro similarity) {
        log.debug("Consumed similarity: eventA={}, eventB={}, score={}",
                similarity.getEventA(), similarity.getEventB(), similarity.getScore());
        analyzerService.saveEventSimilarity(similarity);
    }
}
