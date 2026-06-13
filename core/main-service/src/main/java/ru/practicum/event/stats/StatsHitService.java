package ru.practicum.event.stats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.StatsClient;
import ru.practicum.dto.request.EndPointHitDtoNew;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsHitService {

    private final StatsClient statsClient;

    @Value("${evm.app-name:evm-main-service}")
    private String appName;

    //отправляет hit в stats-service, если он не доступен, пишем предупрреждение и не даем приложению упасть
    public void sendHit(String uri, String ip) {
        EndPointHitDtoNew hitDto = EndPointHitDtoNew.builder()
                .app(appName)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();
        try {
            statsClient.hit(hitDto);
        } catch (Exception e) {
            log.warn("Не удалось отправить hit в stats-service: {}", e.getMessage());
        }
    }
}
