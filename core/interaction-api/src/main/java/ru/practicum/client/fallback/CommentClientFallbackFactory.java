package ru.practicum.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.client.CommentClient;
import ru.practicum.dto.comment.CommentEventDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CommentClientFallbackFactory implements FallbackFactory<CommentClient> {

    @Override
    public CommentClient create(Throwable cause) {
        return new CommentClient() {

            @Override
            public List<CommentEventDto> getCommentsForEvent(Long eventId) {
                log.error("Comment service недоступна. Возвращаю пустой список комментариев для события {}",
                        eventId, cause);
                return Collections.emptyList();
            }

            @Override
            public Map<Long, List<CommentEventDto>> getCommentsForEvents(List<Long> eventIds) {
                log.error("Comment service недоступна. Возвращаю пустую мапу комментариев для событий {}",
                        eventIds, cause);

                return eventIds.stream()
                        .collect(Collectors.toMap(
                                Function.identity(),
                                id -> Collections.emptyList()
                        ));
            }
        };
    }
}
