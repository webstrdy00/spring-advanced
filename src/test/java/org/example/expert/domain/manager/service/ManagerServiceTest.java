package org.example.expert.domain.manager.service;

import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.manager.repository.ManagerRepository;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private ManagerRepository managerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TodoRepository todoRepository;
    @InjectMocks
    private ManagerService managerService;

    @Nested
    class saveManagerTest{
        @Test // 테스트코드 샘플
        void todo가_정상적으로_등록된다() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);  // 일정을 만든 유저

            long todoId = 1L;
            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);

            long managerUserId = 2L;
            User managerUser = new User("b@b.com", "password", UserRole.USER);  // 매니저로 등록할 유저
            ReflectionTestUtils.setField(managerUser, "id", managerUserId);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId); // request dto 생성

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(userRepository.findById(managerUserId)).willReturn(Optional.of(managerUser));
            given(managerRepository.save(any(Manager.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            ManagerSaveResponse response = managerService.saveManager(authUser, todoId, managerSaveRequest);

            // then
            assertNotNull(response);
            assertEquals(managerUser.getId(), response.getUser().getId());
            assertEquals(managerUser.getEmail(), response.getUser().getEmail());
        }

        @Test
        void todo의_user가_null인_경우_예외가_발생한다() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            long todoId = 1L;
            long managerUserId = 2L;

            Todo todo = new Todo();
            ReflectionTestUtils.setField(todo, "user", null);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.saveManager(authUser, todoId, managerSaveRequest)
            );

            assertEquals("Todo에 연관된 사용자 정보가 없습니다.", exception.getMessage());
        }

        @Test
        void 유효하지_않은_사용자가_담당자_등록_시도_시_예외가_발생한다(){
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            long todoId = 1L;
            long managerUserId = 2L;

            User todoOwner = new User("owner@example.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(todoOwner, "id", 3L);

            Todo todo = new Todo("Title", "content", "Sunny", todoOwner);
            ReflectionTestUtils.setField(todo, "id", todoId);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.saveManager(authUser, todoId, managerSaveRequest));
            assertEquals("담당자를 등록하려고 하는 유저가 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());
        }

        @Test
        void 존재하지_않은_담당자_유저_등록_시도_시_예외가_발생한다(){
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            long todoId = 1L;
            long managerUserId = 2L;

            User todoOwner = User.fromAuthUser(authUser);
            Todo todo = new Todo("Title", "content", "Sunny", todoOwner);
            ReflectionTestUtils.setField(todo, "id", todoId);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(userRepository.findById(managerUserId)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.saveManager(authUser, todoId, managerSaveRequest));

            assertEquals("등록하려고 하는 담당자 유저가 존재하지 않습니다.", exception.getMessage());
        }

        @Test
        void 일정_작성자가_자신을_담당자로_등록_시도_시_예외가_발생한다(){
            // given
            long userId = 1L;
            AuthUser authUser = new AuthUser(userId, "a@a.com", UserRole.USER);
            long todoId = 1L;

            User todoOwner = User.fromAuthUser(authUser);
            Todo todo = new Todo("Title", "content", "Sunny", todoOwner);
            ReflectionTestUtils.setField(todo, "id", todoId);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(userId);


            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(userRepository.findById(userId)).willReturn(Optional.of(todoOwner));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.saveManager(authUser, todoId, managerSaveRequest));

            assertEquals("일정 작성자는 본인을 담당자로 등록할 수 없습니다.", exception.getMessage());
        }
    }

    @Nested
    class getManagerTest{
        @Test // 테스트코드 샘플
        public void manager_목록_조회에_성공한다() {
            // given
            long todoId = 1L;
            User user = new User("user1@example.com", "password", UserRole.USER);
            Todo todo = new Todo("Title", "Contents", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);

            Manager mockManager = new Manager(todo.getUser(), todo);
            List<Manager> managerList = List.of(mockManager);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(managerRepository.findByTodoIdWithUser(todoId)).willReturn(managerList);

            // when
            List<ManagerResponse> managerResponses = managerService.getManagers(todoId);

            // then
            assertEquals(1, managerResponses.size());
            assertEquals(mockManager.getId(), managerResponses.get(0).getId());
            assertEquals(mockManager.getUser().getEmail(), managerResponses.get(0).getUser().getEmail());
        }

        @Test
        public void manager_목록_조회_시_Todo가_없다면_InvalidRequestException_에러를_던진다() {
            // given
            long todoId = 1L;
            given(todoRepository.findById(todoId)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> managerService.getManagers(todoId));
            assertEquals("Todo not found", exception.getMessage());
        }
    }

    @Nested
    class deleteManagerTest{
        @Test
        void 담당자_삭제에_성공한다(){
            // given
            long userId = 1L;
            long todoId = 1L;
            long managerId = 1L;

            User user = new User("user1@example.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", userId);

            Todo todo = new Todo("Title", "content", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);

            Manager manager = new Manager(user, todo);
            ReflectionTestUtils.setField(manager, "id", managerId);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(managerRepository.findById(managerId)).willReturn(Optional.of(manager));

            // when
            managerService.deleteManager(userId, todoId, managerId);

            // then
            verify(managerRepository).delete(manager);
        }
        @Test
        void 존재하지_않은_유저로_담당자_삭제_시도_시_예외가_발생한다(){
            // given
            long userId = 1L;
            long todoId = 1L;
            long managerId = 1L;

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.deleteManager(userId, todoId, managerId));
            assertEquals("User not found", exception.getMessage());
        }
        @Test
        void 존재하지_않은_Todo로_담당자_삭제_시도_시_예외가_발생한다(){
            // given
            long userId = 1L;
            long todoId = 1L;
            long managerId = 1L;

            User user = new User("user1@example.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", userId);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(todoRepository.findById(todoId)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.deleteManager(userId, todoId, managerId));
            assertEquals("Todo not found", exception.getMessage());
        }
        @Test
        void Todo의_User가_null일_때_예외가_발생한다(){
            // given
            long userId = 1L;
            long todoId = 1L;
            long managerId = 1L;

            User user = new User("user1@example.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", userId);

            Todo todo = new Todo("Title", "content", "Sunny", null);
            ReflectionTestUtils.setField(todo, "id", todoId);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.deleteManager(userId, todoId, managerId));
            assertEquals("해당 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());
        }
        @Test
        void Todo의_User와_요청한_User가_다를_때_예외가_발생한다(){
            // given
            long userId = 1L;
            long todoId = 1L;
            long managerId = 1L;

            User user = new User("user@example.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", userId);

            User otherUser = new User("other@example.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(otherUser, "id", 2L);


            Todo todo = new Todo("Title", "Contents", "Sunny", otherUser);
            ReflectionTestUtils.setField(todo, "id", todoId);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.deleteManager(userId, todoId, managerId));
            assertEquals("해당 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());
        }
        @Test
        void 존재하지_않은_Manager로_담당자_삭제_시도_시_예외가_발생한다(){
            // given
            long userId = 1L;
            long todoId = 1L;
            long managerId = 1L;

            User user = new User("user1@example.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", userId);

            Todo todo = new Todo("Title", "content", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.deleteManager(userId, todoId, managerId));
            assertEquals("Manager not found", exception.getMessage());
        }

        @Test
        void Manager가_해당_Todo에_등록되지_않았을_때_예외가_발생한다(){
            // given
            long userId = 1L;
            long todoId = 1L;
            long managerId = 1L;


            User user = new User("user@example.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", userId);

            Todo todo = new Todo("Title", "Contents", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);

            Todo otherTodo = new Todo("Other Title", "Other Contents", "Rainy", user);
            ReflectionTestUtils.setField(otherTodo, "id", 2L);

            Manager manager = new Manager(user, otherTodo);
            ReflectionTestUtils.setField(manager, "id", managerId);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(managerRepository.findById(managerId)).willReturn(Optional.of(manager));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.deleteManager(userId, todoId, managerId));
            assertEquals("해당 일정에 등록된 담당자가 아닙니다.", exception.getMessage());
        }
    }
}
