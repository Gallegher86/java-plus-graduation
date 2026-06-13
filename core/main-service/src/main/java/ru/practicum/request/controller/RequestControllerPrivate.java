package ru.practicum.request.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.service.RequestService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
public class RequestControllerPrivate {
    private final RequestService requestService;

    @PostMapping
    public ResponseEntity<ParticipationRequestDto> create(
            @Positive @PathVariable Long userId,
            @Positive @RequestParam @NotNull Long eventId
    ) {
        ParticipationRequestDto result = requestService.create(userId, eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public ResponseEntity<List<ParticipationRequestDto>> findAll(
            @Positive @PathVariable Long userId
    ) {
        List<ParticipationRequestDto> result = requestService.findAll(userId);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(
            @Positive @PathVariable Long userId,
            @Positive @PathVariable Long requestId
    ) {
        ParticipationRequestDto result = requestService.cancelRequest(userId, requestId);
        return ResponseEntity.ok(result);
    }
}