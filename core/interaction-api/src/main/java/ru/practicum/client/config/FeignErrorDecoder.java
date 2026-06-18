package ru.practicum.client.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import ru.practicum.exception.NotFoundException;

public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {

            if (methodKey.contains("UserClient")) {
                return new NotFoundException("User not found");
            }

            return new NotFoundException("Entity not found");
        }

        return new RuntimeException("Unexpected error");
    }
}
