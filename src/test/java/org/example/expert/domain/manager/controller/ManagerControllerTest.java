package org.example.expert.domain.manager.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.example.expert.config.AuthUserArgumentResolver;
import org.example.expert.config.GlobalExceptionHandler;
import org.example.expert.config.JwtUtil;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.service.ManagerService;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ManagerController.class)
class ManagerControllerTest {
    @Mock
    private AuthUserArgumentResolver resolver;

    @Autowired
    private ManagerController controller;

    @MockBean
    private ManagerService managerService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(resolver)
                .build();
    }

    @Test
    void 매니저_생성에_성공한다() throws Exception {
        // given
        long todoId = 1L;
        AuthUser authUser = new AuthUser(1L, "user@example.com", UserRole.USER);
        ManagerSaveRequest request = new ManagerSaveRequest(2L);
        UserResponse userResponse = new UserResponse(1L,  "user@example.com");
        ManagerSaveResponse response = new ManagerSaveResponse(2L, userResponse);

        given(resolver.supportsParameter(any())).willReturn(true);
        given(resolver.resolveArgument(any(), any(), any(), any())).willReturn(authUser);
        given(managerService.saveManager(any(AuthUser.class), eq(todoId), any(ManagerSaveRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/todos/{todoId}/managers", todoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("authUser", authUser))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.user.id").value(1L))
                .andExpect(jsonPath("$.user.email").value("user@example.com"));

        verify(managerService).saveManager(eq(authUser), eq(todoId), any(ManagerSaveRequest.class));
    }
    @Test
    void 매니저_생성_실패_유효하지_않은_요청() throws Exception {
        // given
        long todoId = 1L;
        ManagerSaveRequest request = new ManagerSaveRequest(null);

        // when & then
        mockMvc.perform(post("/todos/{todoId}/managers", todoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(managerService, never()).saveManager(any(), anyLong(), any());
    }

    @Test
    void 매니저_목록_조회에_성공한다() throws Exception{
        // given
        long todoId = 1L;
        List<ManagerResponse> managerResponses = Arrays.asList(
                new ManagerResponse(1L, new UserResponse(2L, "test1@example.com")),
                new ManagerResponse(2L, new UserResponse(3L, "test2@example.com"))
        );
        given(managerService.getManagers(todoId)).willReturn(managerResponses);

        // when & then
        mockMvc.perform(get("/todos/{todoId}/managers", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].user.id").value(2L))
                .andExpect(jsonPath("$[0].user.email").value("test1@example.com"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].user.id").value(3L))
                .andExpect(jsonPath("$[1].user.email").value("test2@example.com"));

        verify(managerService).getManagers(todoId);
    }
    @Test
    void 매니저_삭제에_성공한다() throws Exception{
        // given
        long todoId = 1L;
        long managerId = 1L;
        AuthUser authUser = new AuthUser(1L, "user@example.com", UserRole.USER);

        given(resolver.supportsParameter(any())).willReturn(true);
        given(resolver.resolveArgument(any(), any(), any(), any())).willReturn(authUser);
        willDoNothing().given(managerService).deleteManager(authUser.getId(), todoId, managerId);

        // when & then
        mockMvc.perform(delete("/todos/{todoId}/managers/{managerId}", todoId, managerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(managerService).deleteManager(authUser.getId(), todoId, managerId);
    }
}