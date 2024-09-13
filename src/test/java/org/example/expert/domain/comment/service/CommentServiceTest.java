package org.example.expert.domain.comment.service;

import org.example.expert.domain.comment.dto.request.CommentSaveRequest;
import org.example.expert.domain.comment.dto.response.CommentResponse;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.comment.repository.CommentRepository;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.common.exception.ServerException;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private TodoRepository todoRepository;
    @InjectMocks
    private CommentService commentService;

    @Nested
    class saveCommentTest{
        @Test
        public void comment_등록_중_할일을_찾지_못해_에러가_발생한다() {
            // given
            long todoId = 1;
            CommentSaveRequest request = new CommentSaveRequest("contents");
            AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);

            given(todoRepository.findById(anyLong())).willReturn(Optional.empty());

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                commentService.saveComment(authUser, todoId, request);
            });

            // then
            assertEquals("Todo not found", exception.getMessage());
        }

        @Test
        public void 할일의_담당자가_아닌_경우_comment_등록_시_에러가_발생한다(){
            // given
            long todoId = 1L;
            CommentSaveRequest request = new CommentSaveRequest("contents");
            AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);
            User user = User.fromAuthUser(authUser);
            User managerUser = new User("manager@example.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(managerUser, "id", 2L);

            Todo todo = new Todo("title", "title", "contents", managerUser);
            ReflectionTestUtils.setField(todo, "id", todoId);

            // manager 리스트 초기화 및 설정
            List<Manager> managers = new ArrayList<>();
            managers.add(new Manager(managerUser, todo));
            ReflectionTestUtils.setField(todo, "managers", managers);

            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->{
                commentService.saveComment(authUser, todoId, request);
            });

            // then
            assertEquals("관리자만 댓글을 추가할 수 있습니다.", exception.getMessage());
        }

        @Test
        public void comment를_정상적으로_등록한다() {
            // given
            long todoId = 1;
            CommentSaveRequest request = new CommentSaveRequest("contents");
            AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);
            User user = User.fromAuthUser(authUser);
            Todo todo = new Todo("title", "title", "contents", user);
            ReflectionTestUtils.setField(todo, "id", todoId);
            Comment comment = new Comment(request.getContents(), user, todo);

            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
            given(commentRepository.save(any())).willReturn(comment);

            // when
            CommentSaveResponse result = commentService.saveComment(authUser, todoId, request);

            // then
            assertNotNull(result);
        }
    }
    @Nested
    class getComments{
        @Test
        void 정상적으로_댓글_목록을_반환한다(){
            // given
            long todoId = 1L;

            User user1 = new User("user1@example.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(user1, "id", 1L);

            User user2 = new User("user2@example.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(user2, "id", 2L);

            Todo todo = new Todo("Test Todo", "Test Content", "Sunny", user1);
            ReflectionTestUtils.setField(todo, "id", todoId);

            Comment comment1 = new Comment("Comment 1", user1, todo);
            ReflectionTestUtils.setField(comment1, "id", 1L);

            Comment comment2 = new Comment("Comment 2", user2, todo);
            ReflectionTestUtils.setField(comment2, "id", 2L);

            List<Comment> commentList = Arrays.asList(comment1, comment2);

            given(commentRepository.findByTodoIdWithUser(todoId)).willReturn(commentList);

            // when
            List<CommentResponse> result = commentService.getComments(todoId);

            // then
            verify(commentRepository).findByTodoIdWithUser(todoId);

            assertEquals(2, result.size());

            CommentResponse response1 = result.get(0);
            assertEquals(1L, response1.getId());
            assertEquals("Comment 1", response1.getContents());
            assertEquals(1L, response1.getUser().getId());
            assertEquals("user1@example.com", response1.getUser().getEmail());

            CommentResponse response2 = result.get(1);
            assertEquals(2L, response2.getId());
            assertEquals("Comment 2", response2.getContents());
            assertEquals(2L, response2.getUser().getId());
            assertEquals("user2@example.com", response2.getUser().getEmail());
        }

        @Test
        void 댓글이_없는_경우_빈_리스트를_반환한다(){
            // given
            long todoId = 1L;

            given(commentRepository.findByTodoIdWithUser(todoId)).willReturn(List.of());

            // when
            List<CommentResponse> result = commentService.getComments(todoId);

            // then
            verify(commentRepository).findByTodoIdWithUser(todoId);

            assertTrue(result.isEmpty());
        }
    }
}
