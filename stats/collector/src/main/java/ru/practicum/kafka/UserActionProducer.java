package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionProducer {
    private final KafkaTemplate<Long, UserActionAvro> kafkaTemplate;

    @Value("${kafka.topics.user-actions}")
    private String topic;

    public void send(UserActionAvro action) {
        kafkaTemplate.send(topic, action.getUserId(), action)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error(
                                "Не удалось отправить UserAction: userId={}, eventId={}",
                                action.getUserId(),
                                action.getEventId(),
                                ex
                        );
                        return;
                    }

                    log.debug(
                            "UserAction отправлен: topic={}, partition={}, offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}
