package com.modu.office.controller;

import com.modu.office.dto.response.NotificationResponse;
import com.modu.office.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "알림 API", description = "알림 조회 및 처리 API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "내 알림 목록 조회", description = "현재 사용자의 알림 목록을 페이징하여 조회합니다.")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.getMyNotifications(userDetails.getUsername(), pageable));
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "알림 SSE 구독", description = "Server-Sent Events 방식을 사용하여 알림을 실시간으로 구독합니다.")
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails userDetails) {
        return notificationService.subscribe(userDetails.getUsername());
    }

    @GetMapping(value = "/subscribe/rooms/{roomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "회의실 캘린더 SSE 구독", description = "특정 회의실의 달력 화면을 실시간으로 구독합니다.")
    public SseEmitter subscribeRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        // 인가(Authentication) 처리: 로그인한 사용자만 접근 가능하도록 @AuthenticationPrincipal 사용
        // 추가적인 접근 권한 로직(특정 오피스 소속 여부 등)이 필요하면 여기서 처리할 수 있습니다.
        return notificationService.subscribeRoom(roomId);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 알림 개수 조회", description = "새로 들어온 알림 개수를 조회합니다.")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userDetails.getUsername()));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "단일 알림 읽음 처리", description = "단일 알림을 읽음으로 표시합니다.")
    @ApiResponse(responseCode = "204", description = "읽음 처리 성공")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAsRead(notificationId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "모든 알림 읽음 처리", description = "유저의 모든 읽지 않은 알림을 읽음으로 표시합니다.")
    @ApiResponse(responseCode = "204", description = "전체 읽음 처리 성공")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
