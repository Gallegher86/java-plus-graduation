package ru.practicum.facade;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.comment.*;

import java.util.List;
import java.util.Map;

public interface CommentFacade {
    CommentShortDto createComment(Long userId, Long eventId, NewCommentDto dto);

    CommentShortDto updateComment(Long userId, Long commentId, NewCommentDto request);

    void deleteByUser(Long userId, Long commentId);

    List<CommentShortDto> approveComments(CommentStatusUpdateRequest request);

    List<CommentFullDto> getCommentsForAdmin(AdminCommentFilterParams params, Pageable pageable);

    List<CommentEventDto> getCommentsForEvent(Long eventId);

    Map<Long, List<CommentEventDto>> getCommentsForEvents(List<Long> eventIds);

    CommentFullDto getCommentById(Long commentId);

    void deleteByAdmin(Long commentId);
}
