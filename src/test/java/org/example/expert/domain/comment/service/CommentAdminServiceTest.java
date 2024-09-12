package org.example.expert.domain.comment.service;

import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.comment.repository.CommentRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentAdminServiceTest {
    @Mock
    private CommentRepository commentRepository;
    @InjectMocks
    private CommentAdminService commentAdminService;

    @Nested
    class deleteCommentTest{
        @Test
        void 정상적으로_댓글을_삭제한다(){
            // given
            long commentId = 1L;

            // when
            commentAdminService.deleteComment(commentId);

            // then
            verify(commentRepository).deleteById(commentId);
            assertTrue(true, "댓글이 성공적으로 삭제되었습니다.");
        }
    }
}