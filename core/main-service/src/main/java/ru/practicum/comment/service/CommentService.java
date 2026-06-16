package ru.practicum.comment.service;

import ru.practicum.comment.dto.*;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentService {
    CommentShortDto createComment(Long userId, Long eventId, NewCommentDto dto);

    CommentShortDto updateComment(Long userId, Long commentId, NewCommentDto request);

    void deleteByUser(Long userId, Long commentId);

    List<CommentShortDto> approveComments(CommentStatusUpdateRequest request);

    List<CommentFullDto> getComments(AdminCommentFilterParams params, Pageable pageable);

    CommentFullDto getCommentById(Long commentId);

    void deleteByAdmin(Long commentId);
}
