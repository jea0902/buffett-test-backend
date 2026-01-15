package com.buffettTest.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "NOTIFICATION")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "NOTI_SEQ_GEN")
    @SequenceGenerator(name = "NOTI_SEQ_GEN", sequenceName = "NOTI_SEQ", allocationSize = 1)
    @Column(name = "NOTI_ID")
    private Long notiId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user; // 알림 수신자

    @Column(nullable = false, length = 20)
    private String type; // TRADE, BOARD, SYSTEM

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "RELATED_URL", length = 500)
    private String relatedUrl;

    @Builder.Default
    @Column(name = "IS_READ", length = 1)
    private String isRead = "N";

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}