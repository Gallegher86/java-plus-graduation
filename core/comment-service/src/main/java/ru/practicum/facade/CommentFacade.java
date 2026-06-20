package ru.practicum.facade;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.comment.*;

import java.util.List;

public interface CommentFacade {
    CommentShortDto createComment(Long userId, Long eventId, NewCommentDto dto);

    CommentShortDto updateComment(Long userId, Long commentId, NewCommentDto request);

    void deleteByUser(Long userId, Long commentId);

    List<CommentShortDto> approveComments(CommentStatusUpdateRequest request);

    List<CommentFullDto> getComments(AdminCommentFilterParams params, Pageable pageable);

    CommentFullDto getCommentById(Long commentId);

    void deleteByAdmin(Long commentId);
}
