package com.buffettTest.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal; // 소수점 계산 오차 없는 엄격한 숫자 타입
import java.time.LocalDateTime;

@Entity
@Table(name = "STOCK_OF_USA")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StockOfUsa {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "STOCK_SEQ_GEN")
    @SequenceGenerator(name = "STOCK_SEQ_GEN", sequenceName = "STOCK_SEQ", allocationSize = 1)
    @Column(name = "STOCK_ID")
    private Long stockId;

    @Column(unique = true, nullable = false)
    private String ticker;

    @Column(name = "COMPANY_NAME", nullable = false)
    private String companyName;

    private String exchange;
    private String industry;
    private String indexType; // 지수 포함 여부

    // @Column(precision = 5, scale = 2)
    // private BigDecimal buffettScore; // 소수점 둘째자리까지(정교한 계산)
    @Column(precision = 5, scale = 2)
    private BigDecimal qualityScore; // 우량도 점수

    @Builder.Default
    @Column(columnDefinition = "CHAR(1)")
    private String isQualified = "N"; // 'Y' or 'N' (필터용)

    private String priceStatus; // "ROWVALUED(황금색)" or "OVERVALUED(빨간색)"

    @Column(precision = 20, scale = 4)
    private BigDecimal targetPrice; // 목표가 (사용자에게 텍스트로 보여줄 값)

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

}
