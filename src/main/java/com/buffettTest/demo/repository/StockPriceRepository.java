package com.buffettTest.demo.repository;

import com.buffettTest.demo.entity.StockPrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {
    // PK가 부모와 같으므로 findById(stockId)만으로도 즉시 조회가 가능합니다.
}