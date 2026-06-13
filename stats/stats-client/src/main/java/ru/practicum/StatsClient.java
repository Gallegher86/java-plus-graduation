package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;
import ru.practicum.dto.request.ViewStatsParamDto;
import ru.practicum.exceptions.StatsClientException;

import ru.practicum.dto.request.EndPointHitDtoNew;
import ru.practicum.dto.response.ViewStatsDto;
import ru.practicum.exceptions.StatsServerUnavailable;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class StatsClient {
    private static final String SERVICE_ID = "stats-server";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final RestClient restClient;

    @Autowired
    public StatsClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;

        this.retryTemplate = createRetryTemplate();

        this.restClient = RestClient.builder()
                .defaultStatusHandler(
                        HttpStatusCode::isError,
                        (request, response) -> {
                            String body = new String(
                                    response.getBody().readAllBytes(),
                                    StandardCharsets.UTF_8
                            );

                            log.warn(
                                    "Ошибка вызова Stats-client, status: {}, body: {}",
                                    response.getStatusCode(),
                                    body
                            );

                            throw new StatsClientException(response.getStatusCode(), body);
                        }
                )
                .build();
    }

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
        log.info("Stats-server ответил на запрос статистики от Stats-client, статус ответа: {}:", status);
        return body;
    }

    private ServiceInstance getInstance() {
        return discoveryClient.getInstances(SERVICE_ID)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new StatsServerUnavailable("Stats-server не найден в discovery: " + SERVICE_ID)
                );
    }

    private String baseUrl() {
        return retryTemplate.execute(
                context -> getInstance().getUri().toString()
        );
    }

    private RetryTemplate createRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
