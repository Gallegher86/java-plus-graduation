package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;
import ru.practicum.dto.request.ViewStatsParamDto;

import ru.practicum.dto.request.EndPointHitDtoNew;
import ru.practicum.dto.response.ViewStatsDto;
import ru.practicum.exceptions.StatsServerUnavailable;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClient {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;
    private final RetryTemplate retryTemplate;
    private final StatsClientProperties properties;

    public ResponseEntity<Void> hit(EndPointHitDtoNew hitDto) {
        log.info("Stats-client получен запрос на отправку данных - hitDto: {}", hitDto);
        ResponseEntity<Void> response = restClient.post()
                .uri(baseUrl() + "/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(hitDto)
                .retrieve()
                .toBodilessEntity();
        log.info("Stats-server ответил на запрос Stats-client, статус ответа: {}", response.getStatusCode());
        return response;
    }

    public List<ViewStatsDto> get(ViewStatsParamDto paramDto) {
        log.info("Stats-client получен запрос на получение статистики с параметрами: {}", paramDto);
        ResponseEntity<List<ViewStatsDto>> response = restClient.get()
                .uri(baseUrl() + "/stats", uriBuilder -> uriBuilder
                        .queryParam("start", paramDto.getStart().format(FORMATTER))
                        .queryParam("end", paramDto.getEnd().format(FORMATTER))
                        .queryParamIfPresent("uris",
                                Optional.ofNullable(
                                        CollectionUtils.isEmpty(paramDto.getUris())
                                                ? null
                                                : paramDto.getUris()
                                ))
                        .queryParam("unique", paramDto.getUnique())
                        .build()
                )
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        HttpStatusCode status = response.getStatusCode();
        List<ViewStatsDto> body = response.getBody();
        log.info("Stats-server ответил на запрос статистики от Stats-client, статус ответа: {}.", status);
        return body;
    }

    private ServiceInstance getInstance() {
        String serviceId = properties.getServiceId();

        ServiceInstance instance = loadBalancerClient.choose(serviceId);

        if (instance == null) {
            throw new StatsServerUnavailable("Stats-server не найден: " + serviceId);
        }

        return instance;
    }

    private String baseUrl() {
        return retryTemplate.execute(
                context -> getInstance().getUri().toString()
        );
    }
}
