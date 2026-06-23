package ru.practicum.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.event.model.Event;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class})
public interface EventMapper {

    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "initiator", source = "initiator")
    EventShortDto toEventShortDto(Event event, UserShortDto initiator);
}
