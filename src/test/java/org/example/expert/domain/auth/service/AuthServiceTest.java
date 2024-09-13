package org.example.expert.domain.auth.service;

import org.example.expert.config.JwtUtil;
import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.exception.InvalidRequestException;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("signup 테스트")
    class SignupTest{
        @Test
        void 정상적으로_회원가입이_성공한다(){
            // given
            SignupRequest request = new SignupRequest("test@example.com", "password", "USER");
            User savedUser = new User("test@example.com", "encodedPassword", UserRole.USER);
            String token = "Bearer token";

            given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
            given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(jwtUtil.createToken(any(), any(), any())).willReturn(token);

            // when
            SignupResponse response = authService.signup(request);

            // then
            assertNotNull(response);
            assertEquals(token, response.getBearerToken());
            verify(userRepository).save(any(User.class));
        }
        @Test
        void 이메일이_null이거나_비어있을_경우_예외가_발생한다(){
            // given
            SignupRequest request = new SignupRequest(null, "password", "USER");

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    authService.signup(request));
            assertEquals("이메일은 필수 입력 항목입니다.", exception.getMessage());
        }

        @Test
        void 존재하는_이메일로_회원가입_시_예외가_발생한다(){
            // given
            SignupRequest request = new SignupRequest("test@example.com", "password", "USER");
            given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    authService.signup(request));
            assertEquals("이미 존재하는 이메일입니다.", exception.getMessage());
        }

        @Test
        void 비밀번호가_null이거나_비어있을_경우_예외가_발생한다(){
            // given
            SignupRequest request = new SignupRequest("test@example.com", null, "USER");

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    authService.signup(request));
            assertEquals("비밀번호는 필수 입력 항목입니다.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("signin 테스트")
    class signinTest{
        @Test
        void 정상적으로_로그인이_진행된다(){
            // given
            SigninRequest request = new SigninRequest("test@example.com", "password");
            User user = new User("test@example.com", "encodedPassword", UserRole.USER);
            String token = "Bearer token";

            given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.getPassword(), user.getPassword())).willReturn(true);
            given(jwtUtil.createToken(any(), any(), any())).willReturn(token);

            // when
            SigninResponse response = authService.signin(request);

            // then
            assertNotNull(response);
            assertEquals(token, response.getBearerToken());
        }

        @Test
        void 존재하는_이메일로_로그인_시_예외가_발생한다(){
            // given
            SigninRequest request = new SigninRequest("test@example.com", "password");

            given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    authService.signin(request));
            assertEquals("가입되지 않은 유저입니다.", exception.getMessage());
        }

        @Test
        void 잘못된_비밀번호로_로그인_시_예외가_발생한다(){
            // given
            SigninRequest request = new SigninRequest("test@example.com", "password");
            User user = new User("test@example.com", "encodedPassword", UserRole.USER);

            given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.getPassword(), user.getPassword())).willReturn(false);

            // when & then
            AuthException exception = assertThrows(AuthException.class, () -> authService.signin(request));
            assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
        }
    }
}