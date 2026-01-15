package com.buffettTest.demo.repository;

import com.buffettTest.demo.entity.StockOfUsa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockOfUsaRepository extends JpaRepository<StockOfUsa, Long> {
    // 티커로 찾기
    Optional<StockOfUsa> findByTicker(String ticker);

    // [중요] 90점 이상(Y)인 종목만 우량도 높은 순으로 가져오기
    List<StockOfUsa> findByIsQualifiedOrderByQualityScoreDesc(String isQualified);

    // 종목이 많은 경우 서버가 힘들어질 수 있기에 service에서 findAll이 아니라 repo에서 황금색만 가져오게 설계
    List<StockOfUsa> findByIsQualifiedAndPriceStatusOrderByQualityScoreDesc(String isQualified, String priceStatus);

}