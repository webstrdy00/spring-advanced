package org.example.expert.domain.todo.service;

import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.todo.service.TodoService;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TodoServiceTest {
    @Mock
    private TodoRepository todoRepository;
    @Mock
    private WeatherClient weatherClient;

    @InjectMocks
    private TodoService todoService;

    private AuthUser authUser;
    private User user;
    @Spy
    private Todo todo;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser(1L, "test@example.com", UserRole.USER);
        user = User.fromAuthUser(authUser);
        todo = new Todo("테스트 할일", "테스트 내용", "맑음", user);
        todo.setId(1L);
    }
    @Nested
    @DisplayName("할일 저장 테스트")
    class saveTodoTest{
        @Test
        @DisplayName("새로운 할일 저장 테스트")
        public void testSaveTodo(){
            // given
            TodoSaveRequest request = new TodoSaveRequest("테스트 할일", "테스트 내용");
            given(weatherClient.getTodayWeather()).willReturn("맑음");
            given(todoRepository.save(any(Todo.class))).willReturn(todo);

            // when
            TodoSaveResponse response = todoService.saveTodo(authUser, request);

            // then
            assertThat(response)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.getId()).isEqualTo(todo.getId());
                        assertThat(r.getTitle()).isEqualTo(todo.getTitle());
                        assertThat(r.getContents()).isEqualTo(todo.getContents());
                        assertThat(r.getWeather()).isEqualTo("맑음");
                        assertThat(r.getUser().getId()).isEqualTo(user.getId());
                        assertThat(r.getUser().getEmail()).isEqualTo(user.getEmail());
                    });

            verify(weatherClient).getTodayWeather();
            verify(todoRepository).save(any(Todo.class));
        }
    }
    @Nested
    @DisplayName("할일 목록 조회 테스트")
    class getTodosTest{
        @Test
        @DisplayName("할일 목록 조회 성공 테스트")
        public void testGetTodos(){
            // given
            Page<Todo> todoPage = new PageImpl<>(Arrays.asList(todo));
            given(todoRepository.findAllByOrderByModifiedAtDesc(any(PageRequest.class))).willReturn(todoPage);

            // when
            Page<TodoResponse> response = todoService.getTodos(1, 100);

            // then
            assertThat(response)
                    .isNotNull()
                    .hasSize(1)
                    .first()
                    .satisfies(todoResponse -> {
                        assertThat(todoResponse.getId()).isEqualTo(todo.getId());
                        assertThat(todoResponse.getTitle()).isEqualTo(todo.getTitle());
                        assertThat(todoResponse.getContents()).isEqualTo(todo.getContents());
                        assertThat(todoResponse.getWeather()).isEqualTo(todo.getWeather());
                        assertThat(todoResponse.getUser().getId()).isEqualTo(user.getId());
                        assertThat(todoResponse.getUser().getEmail()).isEqualTo(user.getEmail());
                    });

            verify(todoRepository).findAllByOrderByModifiedAtDesc(any(PageRequest.class));
        }
    }
    @Nested
    @DisplayName("특정 할일 조회 테스트")
   class getTodoTest{
       @Test
       @DisplayName("특정 할일 조회 성공 테스트")
       public void testGetTodo_Success(){
           // given
           given(todoRepository.findByIdWithUser(anyLong())).willReturn(Optional.of(todo));

           // when
           TodoResponse response = todoService.getTodo(1L);

           // then
           assertThat(response)
                   .isNotNull()
                   .satisfies(r -> {
                       assertThat(r.getId()).isEqualTo(todo.getId());
                       assertThat(r.getTitle()).isEqualTo(todo.getTitle());
                       assertThat(r.getContents()).isEqualTo(todo.getContents());
                       assertThat(r.getWeather()).isEqualTo("맑음");
                       assertThat(r.getUser().getId()).isEqualTo(user.getId());
                       assertThat(r.getUser().getEmail()).isEqualTo(user.getEmail());
                   });
           verify(todoRepository).findByIdWithUser(anyLong());
       }

       @Test
       @DisplayName("존재하지 않는 할일 조회 시 예외 발생 테스트")
       public void testGetTodo_NotFound(){
           // given
           given(todoRepository.findByIdWithUser(anyLong())).willReturn(Optional.empty());

           // when & then
           InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                   () -> todoService.getTodo(1L));
           assertEquals("Todo not found", exception.getMessage());
       }
   }
}
