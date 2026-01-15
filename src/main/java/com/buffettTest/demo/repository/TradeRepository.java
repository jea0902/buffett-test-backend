package com.buffettTest.demo.repository;

import com.buffettTest.demo.entity.Trade;
import com.buffettTest.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    // 특정 유저의 거래 내역을 최근 순으로 조회
    List<Trade> findByUserOrderByCreatedAtDesc(User user);

    // 특정 유저의 현재 열려있는(OPEN) 포지션만 조회
    List<Trade> findByUserAndStatus(User user, String status);

    // 특정 티커의 진행 중인 포지션 찾기
    List<Trade> findByTickerAndStatus(String ticker, String status);
}