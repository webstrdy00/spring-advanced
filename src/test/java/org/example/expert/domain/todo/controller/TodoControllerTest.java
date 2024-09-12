package org.example.expert.domain.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.example.expert.config.AuthUserArgumentResolver;
import org.example.expert.config.GlobalExceptionHandler;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.service.TodoService;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TodoController.class)
class TodoControllerTest {
    @Mock
    private AuthUserArgumentResolver resolver;

    private MockMvc mockMvc;

    @Autowired
    private TodoController controller;

    @MockBean
    private TodoService todoService;

    @Autowired
    private ObjectMapper objectMapper;


    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(resolver)
                .build();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    @Test
    void 할일_생성에_성공한다() throws Exception {
        // given
        AuthUser authUser = new AuthUser(1L, "user@example.com", UserRole.USER);
        TodoSaveRequest request = new TodoSaveRequest("Test Todo", "Test Content");
        UserResponse userResponse = new UserResponse(1L, "user@example.com");
        TodoSaveResponse response = new TodoSaveResponse(1L, "Test Todo", "Test Content", "맑음", userResponse);

        given(resolver.supportsParameter(any())).willReturn(true);
        given(resolver.resolveArgument(any(), any(), any(), any())).willReturn(authUser);
        given(todoService.saveTodo(any(AuthUser.class), any(TodoSaveRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Todo"))
                .andExpect(jsonPath("$.contents").value("Test Content"))
                .andExpect(jsonPath("$.weather").value("맑음"))
                .andExpect(jsonPath("$.user.id").value(1L))
                .andExpect(jsonPath("$.user.email").value("user@example.com"));
    }

    @Test
    void 할일_목록_조회에_성공한다() throws Exception {
        // given
        AuthUser authUser = new AuthUser(1L, "user@example.com", UserRole.USER);
        UserResponse userResponse = new UserResponse(1L, "user@example.com");
        LocalDateTime now = LocalDateTime.now();

        TodoResponse todo1 = new TodoResponse(1L, "Todo 1", "Content 1", "맑음", userResponse, now, now);
        TodoResponse todo2 = new TodoResponse(2L, "Todo 2", "Content 2", "맑음", userResponse, now, now);
        Page<TodoResponse> todoPage = new PageImpl<>(List.of(todo1, todo2), PageRequest.of(0,10), 2);

        given(resolver.supportsParameter(any())).willReturn(true);
        given(resolver.resolveArgument(any(), any(), any(), any())).willReturn(authUser);
        given(todoService.getTodos(eq(1), eq(10))).willReturn(todoPage);

        // when & then
        mockMvc.perform(get("/todos")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("Todo 1"))
                .andExpect(jsonPath("$.content[0].contents").value("Content 1"))
                .andExpect(jsonPath("$.content[0].weather").value("맑음"))
                .andExpect(jsonPath("$.content[0].user.id").value(1L))
                .andExpect(jsonPath("$.content[0].user.email").value("user@example.com"))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].modifiedAt").exists())
                .andExpect(jsonPath("$.content[1].id").value(2L))
                .andExpect(jsonPath("$.content[1].title").value("Todo 2"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void 할일_단건_조회에_성공한다() throws Exception {
        // given
        AuthUser authUser = new AuthUser(1L, "user@example.com", UserRole.USER);
        UserResponse userResponse = new UserResponse(1L, "user@example.com");
        long todoId = 1L;
        LocalDateTime now = LocalDateTime.now();
        TodoResponse todo = new TodoResponse(1L, "Todo", "Content", "맑음", userResponse, now, now);

        given(resolver.supportsParameter(any())).willReturn(true);
        given(resolver.resolveArgument(any(), any(), any(), any())).willReturn(authUser);
        given(todoService.getTodo(todoId)).willReturn(todo);

        // when & then
        mockMvc.perform(get("/todos/{todoId}", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(todoId))
                .andExpect(jsonPath("$.title").value("Todo"))
                .andExpect(jsonPath("$.contents").value("Content"))
                .andExpect(jsonPath("$.weather").value("맑음"))
                .andExpect(jsonPath("$.user.id").value(1L))
                .andExpect(jsonPath("$.user.email").value("user@example.com"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.modifiedAt").exists());
    }
}