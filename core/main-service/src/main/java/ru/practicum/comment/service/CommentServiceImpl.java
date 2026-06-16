package ru.practicum.comment.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.*;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.common.EntityFinder;
import ru.practicum.event.dto.State;
import ru.practicum.event.model.Event;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.practicum.comment.model.QComment.comment;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EntityFinder entityFinder;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentShortDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        log.info("CommentService: от пользователя с id {}, получен запрос на добавление комментария: {}.",
                userId, dto);

        User author = entityFinder.getUserOrThrow(userId);
        Event event = entityFinder.getEventOrThrow(eventId);
        LocalDateTime now = LocalDateTime.now();

        if (event.getState() != State.PUBLISHED) {
            throw new ConflictException("Нельзя написать комментарий на неопубликованное событие.");
        }

        Comment comment = commentMapper.toComment(dto);
        comment.setAuthor(author);
        comment.setEvent(event);
        comment.setCreatedOn(now);
        comment.setState(State.PENDING);

        Comment saved = commentRepository.save(comment);
        CommentShortDto result = commentMapper.toCommentShortDto(saved);
        log.info("CommentService: комментарий сохранен: {}", result);
        return result;
    }

    @Override
    @Transactional
    public CommentShortDto updateComment(Long userId, Long commentId, NewCommentDto request) {
        log.info("CommentService: от пользователя с id {}, получен запрос на обновление комментария c id {}.",
                userId, commentId);

        Comment saved = entityFinder.getCommentOrThrow(commentId);

        if (!saved.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("Комментарий с id=" + commentId
                    + " от пользователя " + userId + " не найден, обновление невозможно.");
        }

        if (saved.getState() != State.PENDING && saved.getState() != State.CANCELED) {
            throw new ConflictException(
                    "Комментарий в статусе " + saved.getState() + " нельзя изменить."
            );
        }

        if (request.getText() != null) {
            saved.setText(request.getText());

            if (saved.getState() == State.CANCELED) {
                saved.setState(State.PENDING);
            }
        }

        CommentShortDto result = commentMapper.toCommentShortDto(saved);

        log.info("CommentService: комментарий обновлен: {}.", result);
        return result;
    }

    @Override
    @Transactional
    public void deleteByUser(Long userId, Long commentId) {
        log.info("CommentService: от пользователя с id {}, получен запрос на удаление комментария с id {}.",
                userId, commentId);

        Comment saved = entityFinder.getCommentOrThrow(commentId);

        if (!saved.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("Комментарий с id=" + commentId +
                    " от пользователя " + userId + " не найден, удаление невозможно.");
        }

        commentRepository.delete(saved);
        log.info("CommentService: комментарий с id: {} удален пользователем.", commentId);
    }

    @Override
    @Transactional
    public List<CommentShortDto> approveComments(CommentStatusUpdateRequest request) {
        log.info("CommentService: получен запрос админа на изменение статуса комментариев с id: {}.",
                request.getCommentIds());
        List<Comment> comments = commentRepository.findAllByIdInWithAuthorAndEvent(request.getCommentIds());

        validateCommentsExist(comments, request.getCommentIds());
        validateCommentState(comments);

        State newState = request.getStateAction() == CommentStateActionAdmin.APPROVE_COMMENT
                ? State.PUBLISHED
                : State.CANCELED;

        comments.forEach(c -> c.setState(newState));

        List<CommentShortDto> result = comments.stream()
                .map(commentMapper::toCommentShortDto)
                .toList();

        log.info("CommentService: Обновлен статус {} комментариев.", result.size());
        return result;
    }

    @Override
    public List<CommentFullDto> getComments(AdminCommentFilterParams params, Pageable pageable) {
        log.info("CommentService: получен запрос админа на получение комментариев.");
        BooleanExpression predicate = buildPredicate(params);
        List<Comment> comments = commentRepository.findAll(predicate, pageable).getContent();
        List<CommentFullDto> result = comments.stream().map(commentMapper::toCommentFullDto).toList();
        log.info("CommentService: Выдан список из {} комментариев.", result.size());
        return result;
    }

    @Override
    public CommentFullDto getCommentById(Long commentId) {
        log.info("CommentService: получен запрос админа на получение комментария с id: {}.", commentId);
        Comment saved = entityFinder.getCommentOrThrow(commentId);
        CommentFullDto result = commentMapper.toCommentFullDto(saved);
        log.info("CommentService: комментарий найден: {}.", result);
        return result;
    }

    @Override
    @Transactional
    public void deleteByAdmin(Long commentId) {
        log.info("CommentService: получен запрос админа на удаление комментария с id {}.", commentId);
        Comment saved = entityFinder.getCommentOrThrow(commentId);
        commentRepository.delete(saved);
        log.info("CommentService: комментарий с id: {} удален админом.", commentId);
    }

    private BooleanExpression buildPredicate(AdminCommentFilterParams params) {
        LocalDateTime start = params.getRangeStart();
        LocalDateTime end = params.getRangeEnd();

        if (start != null && end != null && end.isBefore(start)) {
            throw new BadRequestException("Дата конца выборки комментариев " +
                    "не может быть раньше даты начала");
        }

        BooleanExpression predicate = comment.isNotNull();

        if (params.getComments() != null && !params.getComments().isEmpty()) {
            predicate = predicate.and(comment.id.in(params.getComments()));
        }

        if (params.getAuthors() != null && !params.getAuthors().isEmpty()) {
            predicate = predicate.and(comment.author.id.in(params.getAuthors()));
        }

        if (params.getEvents() != null && !params.getEvents().isEmpty()) {
            predicate = predicate.and(comment.event.id.in(params.getEvents()));
        }

        if (params.getStates() != null && !params.getStates().isEmpty()) {
            predicate = predicate.and(comment.state.in(params.getStates()));
        }

        if (params.getText() != null && !params.getText().isEmpty()) {
            predicate = predicate.and(comment.text.containsIgnoreCase(params.getText()));
        }

        if (start != null) {
            predicate = predicate.and(comment.createdOn.goe(start));
        }
        if (end != null) {
            predicate = predicate.and(comment.createdOn.loe(end));
        }

        return predicate;
    }

    private void validateCommentsExist(List<Comment> comments, Set<Long> requestIds) {
        Set<Long> foundIds = comments.stream()
                .map(Comment::getId)
                .collect(Collectors.toSet());

        Set<Long> missingIds = new HashSet<>(requestIds);
        missingIds.removeAll(foundIds);

        if (!missingIds.isEmpty()) {
            throw new NotFoundException("Следующие комментарии не найдены: " + missingIds);
        }
    }

    private void validateCommentState(List<Comment> comments) {
        List<Long> invalidComments = comments.stream()
                .filter(c -> c.getState() != State.PENDING)
                .map(Comment::getId)
                .toList();

        if (!invalidComments.isEmpty()) {
            throw new ConflictException(
                    "Следующие комментарии нельзя модерировать, так как они не в статусе PENDING: "
                            + invalidComments
            );
        }
    }
}
