package org.example.expert.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getUser(long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new InvalidRequestException("User not found"));
        return new UserResponse(user.getId(), user.getEmail());
    }

    @Transactional
    public void changePassword(long userId, UserChangePasswordRequest userChangePasswordRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidRequestException("User not found"));

        String newPassword = userChangePasswordRequest.getNewPassword();
        String oldPassword = userChangePasswordRequest.getOldPassword();

        validateNewPassword(newPassword, user.getPassword());
        validateOldPassword(oldPassword, user.getPassword());

        user.changePassword(passwordEncoder.encode(newPassword));
    }

    private void validateNewPassword(String newPassword, String currentEncodedPassword){
        if (!isPasswordValid(newPassword)){
            throw new InvalidRequestException("새 비밀번호는 8자 이상이어야 하고, 숫자와 대문자를 포함해야 합니다.");
        }

        if (passwordEncoder.matches(newPassword, currentEncodedPassword)){
            throw new InvalidRequestException("새 비밀번호는 기존 비밀번호와 같을 수 없습니다.");
        }
    }
    private void validateOldPassword(String oldPassword, String currentEncodePassword){
        if (!passwordEncoder.matches(oldPassword, currentEncodePassword)){
            throw new InvalidRequestException("잘못된 비밀번호입니다.");
        }
    }
    private boolean isPasswordValid(String password){
        return password.length() >= 8 &&
                password.matches(".*\\d.*") &&
                password.matches(".*[A-Z].*");
    }
}
