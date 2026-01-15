package com.buffettTest.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BOARD")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BOARD_SEQ_GEN")
    @SequenceGenerator(name = "BOARD_SEQ_GEN", sequenceName = "BOARD_SEQ", allocationSize = 1)
    @Column(name = "POST_ID")
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user; // 작성자

    @Column(nullable = false, length = 300)
    private String title;

    @Lob // 대용량 텍스트(CLOB) 매핑
    @Column(nullable = false)
    private String content;

    @Builder.Default
    @Column(name = "VIEW_COUNT")
    private Long viewCount = 0L;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;
    // Soft Delete용 : 데이터는 남기되 사용자에게는 안 보여주는 고급 전략.

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}