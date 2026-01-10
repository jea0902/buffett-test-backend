package com.buffettTest.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "STOCK_PRICE")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StockPrice {

    @Id
    @Column(name = "STOCK_ID") // 부모의 ID를 PK로 사용하므로 시퀀스가 필요 없습니다.
    private Long priceId; // 아키텍트님의 규칙을 따르되, 값은 부모의 stockId와 동일하게 들어갑니다.

    // [중요] 1:1 관계 설정
    // @MapsId는 부모의 PK값을 이 엔티티의 PK(priceId)로 사용하겠다는 뜻입니다.
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "STOCK_ID")
    private StockOfUsa stock;

    @Column(name = "CURRENT_PRICE", nullable = false, precision = 20, scale = 4)
    private BigDecimal currentPrice; // 최근 종가 (단위: 달러)

    @Column(precision = 10, scale = 2)
    private BigDecimal per; // 주가수익비율

    @Column(precision = 10, scale = 2)
    private BigDecimal pbr; // 주가순자산비율

    @Column(name = "EARNINGS_YIELD", precision = 10, scale = 2)
    private BigDecimal earningsYield; // 이익수익률 (%)

    @UpdateTimestamp // 데이터가 수정될 때마다 시간이 자동으로 기록됨
    @Column(name = "LAST_UPDATED")
    private LocalDateTime lastUpdated;
}