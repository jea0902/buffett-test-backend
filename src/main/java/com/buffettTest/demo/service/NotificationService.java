package com.buffettTest.demo.service;

import com.buffettTest.demo.entity.Notification;
import com.buffettTest.demo.entity.User;
import com.buffettTest.demo.repository.NotificationRepository;
import com.buffettTest.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * 알림 생성 (다른 서비스에서 호출)
     */
    @Transactional
    public void sendNotification(Long userId, String type, String message, String url) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("알림 수신자를 찾을 수 없습니다."));

        Notification noti = Notification.builder()
                .user(user)
                .type(type) // type컬럼을 통해 '가격 도달' 등 새로운 알림 종류가 추가되어도 대응 가능
                .message(message)
                .relatedUrl(url)
                .isRead("N")
                .build();

        notificationRepository.save(noti);
    }

    /**
     * [추후 추가될 메서드 예시]
     * 특정 유저의 오늘 발생한 좋아요/댓글을 모아서 요약 알림을 생성함
     */
    @Transactional
    public void sendDailySummaryNotification(Long userId, int likeCount, String latestComment) {
        String summaryMessage = String.format("오늘 내 글에 좋아요 %d개와 새 댓글(%s...)이 달렸습니다.",
                likeCount, latestComment);

        // 이미 만들어진 기능을 재사용하면 됨!
        this.sendNotification(userId, "BOARD", summaryMessage, "/my-page/activity");
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notiId) {
        Notification noti = notificationRepository.findById(notiId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));
        noti.setIsRead("Y");
    }

    /**
     * 특정 유저의 알림 목록 조회
     */
    public List<Notification> getMyNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // 'N' 상태(안 읽은 알림)인 알림 개수를 가져옵니다.
    public long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return notificationRepository.countByUserAndIsRead(user, "N");
    }
}