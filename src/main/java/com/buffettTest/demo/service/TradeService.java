package com.buffettTest.demo.service;

import com.buffettTest.demo.entity.Trade;
import com.buffettTest.demo.entity.User;
import com.buffettTest.demo.repository.TradeRepository;
import com.buffettTest.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeService {

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository; // User 객체 조회를 위해 필요

    /**
     * [주문 생성] 시장가 또는 지정가 주문을 통합 처리
     * 트레이딩뷰 신호(?)를 받아 새로운 거래를 생성
     * 
     * @param orderType : "MARKET" 또는 "LIMIT"
     */
    @Transactional
    public Long openPosition(Long userId, String ticker, String side, Integer leverage,
            BigDecimal amount, BigDecimal entryPrice, String orderType) {

        // 1. 유저 정보 조회 (ManyToOne 관계를 맺기 위해 User 객체 필요)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));

        // 2. 강제 청산가 계산 (포지션 방향에 따른 수식 적용)
        BigDecimal liquidationPrice = calculateLiquidationPrice(entryPrice, leverage, side);

        // 3. 상태(Status) 및 오픈 시간 결정
        String status = "PENDING"; // 기본은 대기(LIMIT)
        LocalDateTime openedAt = null;

        if ("MARKET".equalsIgnoreCase(orderType)) {
            status = "OPEN"; // 시장가면 즉시 오픈
            openedAt = LocalDateTime.now();
        }

        // 4. Trade 객체 생성 (Builder 사용)
        Trade trade = Trade.builder()
                .user(user) // 숫자 ID가 아닌 User 객체 자체를 세팅
                .ticker(ticker)
                .positionSide(side)
                .leverage(leverage)
                .amount(amount)
                .entryPrice(entryPrice)
                .liquidationPrice(liquidationPrice)
                .orderType(orderType) // 지정가인지 시장가인지도 DB에 기록에 남기도록
                .status(status) // 위에서 결정된 status로 사용
                .openedAt(openedAt) // 위에서 결정된 시간 사용
                .build();

        return tradeRepository.save(trade).getTradeId();
    }

    /**
     * 강제 청산가 계산 로직
     * LONG: 진입가 * (1 - 1/레버리지)
     * SHORT: 진입가 * (1 + 1/레버리지)
     */
    private BigDecimal calculateLiquidationPrice(BigDecimal entryPrice, Integer leverage, String side) {
        BigDecimal lev = new BigDecimal(leverage);
        // 마진율 = 1 / 레버리지
        BigDecimal marginRate = BigDecimal.ONE.divide(lev, 4, RoundingMode.HALF_UP);

        if ("LONG".equalsIgnoreCase(side)) {
            return entryPrice.multiply(BigDecimal.ONE.subtract(marginRate));
        } else {
            return entryPrice.multiply(BigDecimal.ONE.add(marginRate));
        }
    }

    /**
     * [포지션 종료] 익절/손절 시 호출되어 거래를 마감
     */
    @Transactional
    public void closePosition(Long tradeId, BigDecimal exitPrice) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new RuntimeException("거래 내역을 찾을 수 없습니다."));

        trade.setExitPrice(exitPrice);
        trade.setStatus("CLOSED");
        trade.setClosedAt(LocalDateTime.now());
        // Dirty Checking에 의해 자동 업데이트됨
    }
}