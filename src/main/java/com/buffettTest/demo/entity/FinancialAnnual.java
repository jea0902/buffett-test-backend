package com.buffettTest.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal; // 정밀한 소수점 처리를 위해 사용

@Entity
@Table(name = "FINANCIAL_ANNUAL")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FinancialAnnual {

    @Id // PK 설정
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FINANCIAL_SEQ_GEN")
    @SequenceGenerator(name = "FINANCIAL_SEQ_GEN", sequenceName = "FINANCIAL_SEQ", allocationSize = 1)
    private Long finId;

    // [중요] 외래키 매핑: 숫자 STOCK_ID 대신 객체 자체를 필드로 가집니다.
    @ManyToOne(fetch = FetchType.LAZY) // 여러 재무제표는 하나의 주식에 속함 (N:1)
    @JoinColumn(name = "STOCK_ID", nullable = false) // 실제 DB의 컬럼명
    private StockOfUsa stock; // 자바는 객체 지향이라 객체로 연결합니다.

    @Column(name = "FISCAL_YEAR", nullable = false)
    private Integer fiscalYear; // 회계 연도 (YYYY)

    // 비율 데이터들은 정밀도가 중요하므로 BigDecimal을 사용합니다.
    @Column(name = "ROE_PCT", precision = 10, scale = 2)
    private BigDecimal roePct;

    @Column(name = "ROIC_PCT", precision = 10, scale = 2)
    private BigDecimal roicPct;

    @Column(name = "NET_MARGIN_PCT", precision = 10, scale = 2)
    private BigDecimal netMarginPct;

    // 기초 숫자 데이터들 (단위가 매우 크므로 Long 또는 BigDecimal 사용)
    private Long netIncome; // 당기순이익
    private Long revenue; // 매출액
    private Long operatingIncome; // 영업이익
    private Long totalEquity; // 총 자본
    private Long totalLiabilities; // 총 부채

    @Column(precision = 10, scale = 2)
    private BigDecimal eps; // 주당순이익
}