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

    List<Comment> getComments(AdminCommentFilterParams params, Pageable pageable);

    Comment getCommentById(Long commentId);

    void deleteByAdmin(Long commentId);
}
