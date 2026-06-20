package ru.practicum.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.comment.*;
import ru.practicum.dto.event.EventCommentDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.service.CommentService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentFacadeImpl implements CommentFacade {
    private final EventClient eventClient;
    private final UserClient userClient;
    private final CommentService service;
    private final CommentMapper commentMapper;

    @Override
    public CommentShortDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        log.info("CommentService: от пользователя с id {}, получен запрос на добавление комментария: {}.",
                userId, dto);

        userClient.checkUser(userId);
        EventCommentDto event = eventClient.getEvent(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя написать комментарий на неопубликованное событие.");
        }

        return service.createComment(userId, eventId, dto);
    }

    @Override
    public CommentShortDto updateComment(Long userId, Long commentId, NewCommentDto request) {
        return service.updateComment(userId, commentId, request);
    }

    @Override
    public void deleteByUser(Long userId, Long commentId) {
        service.deleteByUser(userId, commentId);
    }

    @Override
    public List<CommentShortDto> approveComments(CommentStatusUpdateRequest request) {
        return service.approveComments(request);
    }

    @Override
    public List<CommentFullDto> getComments(AdminCommentFilterParams params, Pageable pageable) {
        log.info("CommentService: получен запрос админа на получение комментариев.");
        List<Comment> comments = service.getComments(params, pageable);

        List<Long> userIds = comments.stream()
                .map(Comment::getAuthorId)
                .distinct()
                .toList();

        List<Long> eventIds = comments.stream()
                .map(Comment::getEventId)
                .distinct()
                .toList();

        Map<Long, UserDto> users = userClient.getUsers(userIds)
                .stream()
                .collect(Collectors.toMap(UserDto::getId, u -> u));

        Map<Long, EventCommentDto> events = eventClient.getEvents(eventIds)
                .stream()
                .collect(Collectors.toMap(EventCommentDto::getId, e -> e));

        List<CommentFullDto> result = comments.stream()
                .map(c -> {
                    CommentFullDto dto = commentMapper.toCommentFullDto(c);
                    dto.setAuthor(users.get(c.getAuthorId()));
                    dto.setEvent(events.get(c.getEventId()));
                    return dto;
                })
                .toList();

        log.info("CommentService: Выдан список из {} комментариев.", result.size());
        return result;
    }

    @Override
    public CommentFullDto getCommentById(Long commentId) {
        log.info("CommentService: получен запрос админа на получение комментария с id: {}.", commentId);
        Comment comment = service.getCommentById(commentId);

        UserDto author = userClient.getUser(comment.getAuthorId());
        EventCommentDto event = eventClient.getEvent(comment.getEventId());

        CommentFullDto result = commentMapper.toCommentFullDto(comment);
        result.setAuthor(author);
        result.setEvent(event);
        log.info("CommentService: комментарий с id: {} найден.", commentId);
        return result;
    }

    @Override
    public void deleteByAdmin(Long commentId) {
        service.deleteByAdmin(commentId);
    }
}
