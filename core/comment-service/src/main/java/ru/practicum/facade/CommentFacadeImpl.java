package ru.practicum.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.dto.comment.*;
import ru.practicum.dto.event.EventInternalDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.service.CommentService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentFacadeImpl implements CommentFacade {
    private final EventClientFacade eventClient;
    private final UserClientFacade userClient;
    private final CommentService service;
    private final CommentMapper commentMapper;

    @Override
    public CommentShortDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        log.info("CommentService: от пользователя с id {}, получен запрос на добавление комментария: {}.",
                userId, dto);

        userClient.checkUser(userId);
        EventInternalDto event = eventClient.getEvent(eventId);

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
    public List<CommentFullDto> getCommentsForAdmin(AdminCommentFilterParams params, Pageable pageable) {
        log.info("CommentService: получен запрос админа на получение комментариев.");
        List<Comment> comments = service.getCommentsForAdmin(params, pageable);

        if (comments.isEmpty()) {
            log.info("CommentService: Выдан список из 0 комментариев.");
            return List.of();
        }

        Map<Long, UserDto> users = getUsers(comments);
        Map<Long, EventInternalDto> events = getEvents(comments);

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
    public List<CommentEventDto> getCommentsForEvent(Long eventId) {
        log.info("CommentService: получен запрос на получение комментариев к эвенту {}.", eventId);
        List<Comment> comments = service.getCommentsByEventId(eventId);

        if (comments.isEmpty()) {
            log.info("CommentService: Выдан список из 0 комментариев.");
            return List.of();
        }

        Map<Long, UserDto> users = getUsers(comments);

        List<CommentEventDto> result = comments.stream()
                .map(c -> {
                    CommentEventDto dto = commentMapper.toCommentEventDto(c);
                    dto.setAuthorName(users.get(c.getAuthorId()).getName());
                    return dto;
                })
                .toList();

        log.info("CommentService: Выдан список из {} комментариев.", result.size());
        return result;
    }

    @Override
    public Map<Long, List<CommentEventDto>> getCommentsForEvents(List<Long> eventIds) {
        log.info("CommentService: получен запрос на комментарии для событий {}.", eventIds);

        List<Comment> comments = service.getCommentsByEventIds(eventIds);

        if (comments.isEmpty()) {
            log.info("CommentService: Выдана мапа с 0 комментариев.");
            return Collections.emptyMap();
        }

        Map<Long, UserDto> users = getUsers(comments);

        Map<Long, List<CommentEventDto>> result = comments.stream()
                .map(c -> {
                    CommentEventDto dto = commentMapper.toCommentEventDto(c);
                    dto.setAuthorName(users.get(c.getAuthorId()).getName());
                    return dto;
                })
                .collect(Collectors.groupingBy(CommentEventDto::getEventId));
        log.info("CommentService: Выдана мапа с {} комментариев.", result.size());
        return result;
    }

    @Override
    public CommentFullDto getCommentById(Long commentId) {
        log.info("CommentService: получен запрос админа на получение комментария с id: {}.", commentId);
        Comment comment = service.getCommentById(commentId);

        UserDto author = userClient.getUser(comment.getAuthorId());
        EventInternalDto event = eventClient.getEvent(comment.getEventId());

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

    private Map<Long, UserDto> getUsers(List<Comment> comments) {
        List<Long> userIds = comments.stream()
                .map(Comment::getAuthorId)
                .distinct()
                .toList();

        return userClient.getUsers(userIds)
                .stream()
                .collect(Collectors.toMap(UserDto::getId, u -> u));
    }

    private Map<Long, EventInternalDto> getEvents(List<Comment> comments) {
        List<Long> eventIds = comments.stream()
                .map(Comment::getEventId)
                .distinct()
                .toList();

        return eventClient.getEvents(eventIds)
                .stream()
                .collect(Collectors.toMap(EventInternalDto::getId, e -> e));
    }
}
