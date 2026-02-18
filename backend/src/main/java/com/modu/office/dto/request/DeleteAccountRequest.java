package com.modu.office.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원탈퇴 요청 DTO
 * LOCAL 로그인 사용자는 비밀번호 확인 필요, OAuth2 사용자는 비밀번호 없이 탈퇴 가능
 */
@Getter
@NoArgsConstructor
public class DeleteAccountRequest {

    private String password;
}
