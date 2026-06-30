package ru.practicum.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.comment.*;
import ru.practicum.model.Comment;

import java.util.List;

public interface CommentService {
    CommentShortDto createComment(Long userId, Long eventId, NewCommentDto dto);

    CommentShortDto updateComment(Long userId, Long commentId, NewCommentDto request);

    void deleteByUser(Long userId, Long commentId);

    List<CommentShortDto> approveComments(CommentStatusUpdateRequest request);

    List<Comment> getCommentsForAdmin(AdminCommentFilterParams params, Pageable pageable);

    List<Comment> getCommentsByEventId(Long eventId);

    Comment getCommentById(Long commentId);

    List<Comment> getCommentsByEventIds(List<Long> eventIds);

    void deleteByAdmin(Long commentId);
}
