package com.modu.office.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 즐겨찾기 추가 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddFavoriteRequest {

    @NotNull(message = "회의실 ID는 필수입니다.")
    private Long roomId;
}
