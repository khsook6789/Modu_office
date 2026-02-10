package com.modu.office.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회의실 부대시설 정보를 관리하는 엔티티
 * (예: Wi-Fi, 프로젝터, 화이트보드 등)
 */
@Entity
@Getter
@Table(name = "facility")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Facility extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 시설 식별 코드 (예: "wifi", "projector")
     * DB에서 UNIQUE 제약이 설정되어 있음
     */
    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    /**
     * 사용자에게 표시할 시설명 (예: "무선 인터넷", "빔 프로젝터")
     */
    @Column(name = "label", nullable = false, length = 100)
    private String label;

    /**
     * 시설 활성화 여부
     * false인 경우 신규 회의실에 할당 불가
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public Facility(String name, String label, Boolean isActive) {
        this.name = name;
        this.label = label;
        this.isActive = isActive != null ? isActive : true;
    }

    /**
     * 시설 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 시설 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 시설 정보 수정
     * 
     * @param name     시설 식별 코드
     * @param label    표시명
     * @param isActive 활성화 여부
     */
    public void update(String name, String label, Boolean isActive) {
        this.name = name;
        this.label = label;
        this.isActive = isActive;
    }
}
