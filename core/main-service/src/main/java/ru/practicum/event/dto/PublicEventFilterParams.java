package ru.practicum.event.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

//Параметры публичного поиска событий(GET/events)
@Data
@Builder(toBuilder = true)
public class PublicEventFilterParams {
    //текст для поиска аннотаций
    private String text;

    //список id категорий, по которым ведется поиск
    private List<Long> categories;

    //фоаг платности события
    private Boolean paid;

    //нижняя граница даты события
    //если оба равны нулю, то берем события после текущего момента
    private LocalDateTime rangeStart;

    //верхняя граница даты события
    private LocalDateTime rangeEnd;

    //только события, у которых не исчерпан лимит запросов на участие (пока без фильтрации)
    private Boolean onlyAvailable;

    //вариант сортировки - по дате или по просмотрам
    private EventSort sort;

}
