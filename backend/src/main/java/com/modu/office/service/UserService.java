package com.modu.office.service;

import com.modu.office.dto.request.ChangePasswordRequest;
import com.modu.office.dto.request.DeleteAccountRequest;
import com.modu.office.dto.request.UpdateProfileRequest;
import com.modu.office.dto.response.UserProfileResponse;
import com.modu.office.entity.Account;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.LoginType;
import com.modu.office.exception.ErrorCode;
import com.modu.office.exception.InvalidRequestException;
import com.modu.office.exception.InvalidValueException;
import com.modu.office.repository.AppUserRepository;
import com.modu.office.repository.RefreshTokenRepository;
import com.modu.office.repository.RoomFavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 프로필 관리 서비스
 * - 내 정보 조회/수정, 비밀번호 변경, 회원탈퇴
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoomFavoriteRepository roomFavoriteRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 내 정보 조회
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(AppUser currentUser) {
        AppUser appUser = appUserRepository.findById(java.util.Objects.requireNonNull(currentUser.getId()))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return UserProfileResponse.from(appUser);
    }

    /**
     * 내 정보 수정 (이름 변경)
     */
    @Transactional
    public UserProfileResponse updateProfile(AppUser currentUser, UpdateProfileRequest request) {
        AppUser appUser = appUserRepository.findById(java.util.Objects.requireNonNull(currentUser.getId()))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        appUser.updateName(request.getName());

        return UserProfileResponse.from(appUser);
    }

    /**
     * 비밀번호 변경
     * OAuth2 로그인 사용자는 비밀번호 변경 불가
     */
    @Transactional
    public void changePassword(AppUser currentUser, ChangePasswordRequest request) {
        AppUser appUser = appUserRepository.findById(java.util.Objects.requireNonNull(currentUser.getId()))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Account account = appUser.getAccount();

        if (account.getLoginType() != LoginType.LOCAL) {
            throw new InvalidRequestException(ErrorCode.PASSWORD_CHANGE_NOT_ALLOWED);
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPasswordHash())) {
            throw new InvalidRequestException(ErrorCode.PASSWORD_MISMATCH);
        }

        account.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    /**
     * 회원탈퇴 (소프트 삭제)
     * LOCAL 로그인: 비밀번호 확인 후 탈퇴
     * OAuth2 로그인: 바로 탈퇴 진행
     */
    @Transactional
    public void deleteAccount(AppUser currentUser, DeleteAccountRequest request) {
        AppUser appUser = appUserRepository.findById(java.util.Objects.requireNonNull(currentUser.getId()))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Account account = appUser.getAccount();

        // LOCAL 로그인 사용자는 비밀번호 확인 필요
        if (account.getLoginType() == LoginType.LOCAL) {
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new InvalidValueException(ErrorCode.INVALID_INPUT_VALUE);
            }
            if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
                throw new InvalidRequestException(ErrorCode.PASSWORD_MISMATCH);
            }
        }

        // 1. RefreshToken 삭제
        refreshTokenRepository.deleteByAccount(account);

        // 2. 즐겨찾기 삭제
        roomFavoriteRepository.deleteAllByUserId(appUser.getId());

        // 3. 계정 비활성화 (소프트 삭제)
        account.deactivate();
    }
}
