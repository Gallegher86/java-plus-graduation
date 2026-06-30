package ru.practicum.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.facade.RequestFacade;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
public class RequestPrivateController {
    private final RequestFacade requestFacade;

    @PostMapping
    public ResponseEntity<ParticipationRequestDto> create(
            @Positive @PathVariable Long userId,
            @Positive @RequestParam @NotNull Long eventId
    ) {
        ParticipationRequestDto result = requestFacade.create(userId, eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public ResponseEntity<List<ParticipationRequestDto>> findAll(
            @Positive @PathVariable Long userId
    ) {
        List<ParticipationRequestDto> result = requestFacade.findAll(userId);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(
            @Positive @PathVariable Long userId,
            @Positive @PathVariable Long requestId
    ) {
        ParticipationRequestDto result = requestFacade.cancelRequest(userId, requestId);
        return ResponseEntity.ok(result);
    }
}
