package ru.practicum.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.dto.comment.CommentEventDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.event.model.Event;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class})
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "locLat", source = "location.lat")
    @Mapping(target = "locLon", source = "location.lon")
    @Mapping(target = "partLimit", source = "participantLimit")
    Event toEvent(NewEventDto dto);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests")
    @Mapping(target = "location", source = "event")
    @Mapping(target = "views", source = "views")
    @Mapping(target = "comments", source = "comments")
    @Mapping(target = "initiator", source = "initiator")
    EventFullDto toEventFullDto(Event event,
                                UserShortDto initiator,
                                int confirmedRequests,
                                int views,
                                List<CommentEventDto> comments);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests")
    @Mapping(target = "views", source = "views")
    @Mapping(target = "initiator", source = "initiator")
    EventShortDto toEventShortDto(Event event,
                                  UserShortDto initiator,
                                  int confirmedRequests,
                                  int views);

    default Location toLocation(Event event) {
        return new Location(event.getLocLat(), event.getLocLon());
    }
}
