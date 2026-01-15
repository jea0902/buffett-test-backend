package com.buffettTest.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// 모의 투자 Trade

@Entity
@Table(name = "TRADE")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TRADE_SEQ_GEN")
    @SequenceGenerator(name = "TRADE_SEQ_GEN", sequenceName = "TRADE_SEQ", allocationSize = 1)
    @Column(name = "TRADE_ID")
    private Long tradeId;

    // [수정된 부분] 단순 숫자가 아닌 User 객체와 연결
    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩: 실제 User가 필요할 때만 DB에서 가져옴
    @JoinColumn(name = "USER_ID", nullable = false) // FK 제약 조건과 매핑
    private User user;

    @Builder.Default
    private String ticker = "BTCUSDT";

    @Column(name = "POSITION_SIDE", nullable = false)
    private String positionSide; // "LONG" or "SHORT"

    @Builder.Default
    private Integer leverage = 1;

    @Column(nullable = false)
    private BigDecimal amount; // 투자 원금

    @Column(name = "ENTRY_PRICE", nullable = false)
    private BigDecimal entryPrice;

    @Column(name = "EXIT_PRICE")
    private BigDecimal exitPrice;

    @Column(name = "LIQUIDATION_PRICE")
    private BigDecimal liquidationPrice; // 청산가 (자바에서 계산 예정)

    @Builder.Default
    private String status = "PENDING"; // "PENDING", "OPEN", "CLOSED", "LIQUIDATED"

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "OPENED_AT")
    private LocalDateTime openedAt;

    @Column(name = "CLOSED_AT")
    private LocalDateTime closedAt;

    @Column(name = "ORDER_TYPE", nullable = false)
    private String orderType; // "MARKET" 또는 "LIMIT"

    @PrePersist // 데이터가 처음 들어갈 때 자동으로 현재 시간 설정
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}