package com.modu.office.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 지점 및 회의실 등록 시 시스템 데이터로 표현 불가능한 상세 정보(수칙, 팁 등)를 위한 가이드 템플릿
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DescriptionTemplate {

        /**
         * 지점(Office)용: 위치 상세, 주차 안내, 공지사항 중심
         */
        public static final String OFFICE_DEFAULT_TEMPLATE = "[찾아오시는 길 및 주차 관련 상세]\n\n" +
                        "[지점 이용 수칙 및 공지사항]";

        /**
         * 회의실(Room)용: 이용 예절, 조작 팁 중심
         */
        public static final String ROOM_DEFAULT_TEMPLATE = "[이용 수칙 및 주의사항(예: 뒷정리, 반입 금지 등)]\n\n" +
                        "[공간 이용 팁(예: 냉난방 조절, 조명 등)]";
}
