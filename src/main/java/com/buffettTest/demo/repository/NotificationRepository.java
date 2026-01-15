package com.buffettTest.demo.repository;

import com.buffettTest.demo.entity.Notification;
import com.buffettTest.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 특정 유저의 모든 알림을 최신순으로 조회
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    // 읽지 않은 알림 개수 확인 (배지에 표시용)
    long countByUserAndIsRead(User user, String isRead);

    // 읽지 않은 알림만 조회
    List<Notification> findByUserAndIsReadOrderByCreatedAtDesc(User user, String isRead);
}