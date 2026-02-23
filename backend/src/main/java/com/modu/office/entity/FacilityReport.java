package com.modu.office.entity;

import com.modu.office.entity.enums.ReportIssueType;
import com.modu.office.entity.enums.ReportStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 시설 문제 리포트 엔티티
 */
@Entity
@Getter
@Table(name = "facility_report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FacilityReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private OfficeRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false)
    private ReportIssueType issueType;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status = ReportStatus.REPORTED;

    @Builder
    public FacilityReport(Reservation reservation, OfficeRoom room, Facility facility, ReportIssueType issueType) {
        this.reservation = reservation;
        this.room = room;
        this.facility = facility;
        this.issueType = issueType;
        this.status = ReportStatus.REPORTED;
    }
}
