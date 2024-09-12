package org.example.expert.domain.user.service;

import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    private User testUser;
    private final String oldPassword = "Password123!";
    private final String encodedOldPassword = "encodedPassword123!";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword(encodedOldPassword);
        testUser.setUserRole(UserRole.USER);

        userService = new UserService(userRepository, passwordEncoder);
    }
    @Nested
    @DisplayName("유저 조회 테스트")
    class getUserTest{
        @Test
        @DisplayName("존재하는 사용자 정보 조회시 UserResponse 반환")
        void getUser_ExistingUser_ReturnUserResponse(){
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            UserResponse response = userService.getUser(1L);

            // then
            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("test@example.com", response.getEmail());
            then(userRepository).should().findById(1L);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 정보 조회 시 예외 발생")
        void getUser_NonExistingUser_ThrowsInvalidRequestException(){
            // given
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> userService.getUser(1L));

            assertEquals("User not found", exception.getMessage());
            then(userRepository).should().findById(1L);
        }
    }
    @Nested
    @DisplayName("비밀번호 변경 테스트")
    class ChangePasswordTest{
        @Test
        @DisplayName("유효한 요청으로 비밀번호 변경 시 성공")
        void changePassword_ValidRequest_ChangedPassword(){
            // given
            String newPassword = "newPassword123";
            String newEncodedPassword = "newEncodedPassword123";
            UserChangePasswordRequest request = new UserChangePasswordRequest(oldPassword, newPassword);

            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(newPassword, encodedOldPassword)).willReturn(false);
            given(passwordEncoder.matches(oldPassword, encodedOldPassword)).willReturn(true);
            given(passwordEncoder.encode(newPassword)).willReturn(newEncodedPassword);

            // when
            userService.changePassword(1L, request);

            // then
            then(userRepository).should().findById(1L);
            then(passwordEncoder).should().matches(newPassword, encodedOldPassword);
            then(passwordEncoder).should().matches(oldPassword, encodedOldPassword);
            then(passwordEncoder).should().encode(newPassword);
            assertEquals(newEncodedPassword, testUser.getPassword());
        }

        @Test
        @DisplayName("존재하지 않는 사용자의 비밀번호 변경 시 예외 발생")
        void changePassword_UserNotFound_ThrowsException(){
            // given
            UserChangePasswordRequest request = new UserChangePasswordRequest(oldPassword, "newPassword");
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> userService.changePassword(1L, request));
            assertEquals("User not found", exception.getMessage());
        }

        @Test
        @DisplayName("잘못된 현재 비밀번호로 변경 시도 시 예외 발생")
        void changePassword_IncorrectOldPassword_ThrowsException(){
            // given
            String wrongOldPassword = "wrongPassword";
            String newPassword = "newPassword123";
            UserChangePasswordRequest request = new UserChangePasswordRequest(wrongOldPassword, newPassword);

            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(newPassword, encodedOldPassword)).willReturn(false);
            given(passwordEncoder.matches(wrongOldPassword, encodedOldPassword)).willReturn(false);

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> userService.changePassword(1L, request));
            assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
        }
        @Test
        @DisplayName("새 비밀번호가 유효하지 않을 경우 예외 발생")
        void changePassword_InvalidNewPassword_ThrowsException(){
            String invalidNewPassword = "weak";
            UserChangePasswordRequest request = new UserChangePasswordRequest(oldPassword, invalidNewPassword);

            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> userService.changePassword(1L, request));
            assertEquals("새 비밀번호는 8자 이상이어야 하고, 숫자와 대문자를 포함해야 합니다.", exception.getMessage());
        }

        @Test
        @DisplayName("새 비밀번호가 현재 비밀번호와 같은 경우 예외 발생")
        void changePassword_NewPasswordSameAsOld_ThrowsException(){
            // given
            UserChangePasswordRequest request = new UserChangePasswordRequest(oldPassword, oldPassword);

            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(oldPassword, encodedOldPassword)).willReturn(true);

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> userService.changePassword(1L, request));
            assertEquals("새 비밀번호는 기존 비밀번호와 같을 수 없습니다.", exception.getMessage());
        }
    }

}