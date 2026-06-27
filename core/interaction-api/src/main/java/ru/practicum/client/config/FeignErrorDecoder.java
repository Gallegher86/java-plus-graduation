package ru.practicum.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.error.ApiError;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.InternalServiceException;
import ru.practicum.exception.NotFoundException;

import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    @Override
    public Exception decode(String methodKey, Response response) {

        ApiError apiError = parseError(response);

        String message = (apiError != null && apiError.message() != null)
                ? apiError.message()
                : "Unexpected error";

        return switch (response.status()) {
            case 400 -> new BadRequestException(message);
            case 404 -> new NotFoundException(message);
            case 409 -> new ConflictException(message);
            default -> new InternalServiceException("HTTP " + response.status() + ": " + message);
        };
    }

    private ApiError parseError(Response response) {
        if (response.body() == null) {
            return null;
        }

        try {
            String body = Util.toString(response.body().asReader(StandardCharsets.UTF_8));
            return objectMapper.readValue(body, ApiError.class);
        } catch (Exception e) {
            log.warn("Failed to parse Feign error body: {}", e.getMessage());
            return null;
        }
    }
}
