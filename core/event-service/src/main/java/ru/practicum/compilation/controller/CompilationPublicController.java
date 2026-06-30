package ru.practicum.compilation.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.compilation.facade.CompilationFacade;
import ru.practicum.dto.compilation.CompilationDto;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
public class CompilationPublicController {
    private final CompilationFacade compilationFacade;

    @GetMapping
    public List<CompilationDto> get(@PositiveOrZero @RequestParam(name = "from", defaultValue = "0") int from,
                                    @Positive @RequestParam(name = "size", defaultValue = "10") int size,
                                    @RequestParam(required = false) Boolean pinned) {
        log.info("CompilationControllerPublic: получен запрос на просмотр компиляций, " +
                "начиная с: {}, размером : {}.", from, size);

        Pageable pageable = makePageable(from, size);
        return compilationFacade.get(pageable, pinned);
    }

    @GetMapping("/{compId}")
    public CompilationDto getById(@Positive @PathVariable Long compId) {
        log.info("CompilationControllerPublic: получен запрос на просмотр компиляции с id: {}.", compId);
        return compilationFacade.getById(compId);
    }

    private Pageable makePageable(Integer from, Integer size) {
        return PageRequest.of(
                from / size,
                size);
    }
}
