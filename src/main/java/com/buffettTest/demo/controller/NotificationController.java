package com.buffettTest.demo.controller;

import com.buffettTest.demo.entity.Notification;
import com.buffettTest.demo.service.NotificationService;
import com.buffettTest.demo.entity.User;
import com.buffettTest.demo.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Notification API", description = "사용자 알림 관리 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository; // 알림 개수 조회 시 유저 객체 필요

    @Operation(summary = "나의 알림 목록 조회", description = "로그인한 사용자의 모든 알림을 최신순으로 가져옵니다.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getMyNotifications(@PathVariable Long userId) {
        List<Notification> notifications = notificationService.getMyNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태(IS_READ = 'Y')로 변경합니다.")
    @PatchMapping("/{notiId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long notiId) {
        notificationService.markAsRead(notiId);
        return ResponseEntity.ok("알림이 읽음 처리되었습니다.");
    }

    @Operation(summary = "읽지 않은 알림 개수 조회", description = "상단 바 배지(Badge) 표시를 위한 안 읽은 알림 개수를 반환합니다.")
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<?> getUnreadCount(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 서비스나 레포지토리에 이미 만들어둔 기능을 활용
        // 'N' 상태인 알림 개수를 가져옵니다.
        long count = notificationService.getUnreadCount(userId);

        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}