package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.comment.CommentFullDto;
import ru.practicum.dto.comment.CommentShortDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.model.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "eventId", ignore = true)
    @Mapping(target = "state", ignore = true)
    Comment toComment(NewCommentDto dto);

    CommentShortDto toCommentShortDto(Comment comment);

    @Mapping(target = "author", ignore = true)
    @Mapping(target = "event", ignore = true)
    CommentFullDto toCommentFullDto(Comment comment);
}
