package org.example.expert.domain.user.service;

import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserRoleChangeRequest;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserAdminService userAdminService;

    @Nested
    class changeUserRoleTest{
        @Test
        void 정상적으로_유저_역할을_변경한다(){
            // given
            long userId = 1L;
            User user = new User("user@example.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", userId);

            UserRoleChangeRequest request = new UserRoleChangeRequest("ADMIN");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            userAdminService.changeUserRole(userId, request);

            // then
            verify(userRepository).findById(userId);
            assertEquals(UserRole.ADMIN, user.getUserRole());
        }
        @Test
        void 존재하지_않는_유저ID로_요청시_예외를_발생시킨다(){
            // given
            long nonExistentUserId = 999L;
            UserRoleChangeRequest request = new UserRoleChangeRequest("ADMIN");

            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> userAdminService.changeUserRole(nonExistentUserId, request));
            assertEquals("User not found", exception.getMessage());
        }
    }
}