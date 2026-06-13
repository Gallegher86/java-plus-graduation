package ru.practicum.event.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

//админский поиск событий
@Data
@Builder(toBuilder = true)
public class AdminEventFilterParams {
    //список id пользователей
    private List<Long> users;

    //список состояний событий
    private List<State> states;

    //список id категорий
    private List<Long> categories;

    //не раньше этого времени
    private LocalDateTime rangeStart;

    //не позже этого времени
    private LocalDateTime rangeEnd;

}
