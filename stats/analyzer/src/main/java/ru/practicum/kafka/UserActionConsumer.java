package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.AnalyzerService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {
    private final AnalyzerService analyzerService;

    @KafkaListener(
            topics = "${topic.user-actions}",
            containerFactory = "userActionKafkaListenerContainerFactory"
    )
    public void consume(UserActionAvro action) {
        log.debug("Consumed user action: userId={}, eventId={}", action.getUserId(), action.getEventId());
        analyzerService.saveUserAction(action);
    }
}
