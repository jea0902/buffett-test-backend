package com.buffettTest.demo.service;

import com.buffettTest.demo.entity.StockOfUsa;
import com.buffettTest.demo.repository.StockOfUsaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 미국 전 종목들 최소 필터링(일단 버핏 기준 우량주)해서 노출될 수 있게 하는 로직

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockOfUsaService {

    private final StockOfUsaRepository stockOfUsaRepository;

    /**
     * 사용자가 티커로 검색했을 때 (상세 페이지용)
     */
    public StockOfUsa getStockInfo(String ticker) {
        return stockOfUsaRepository.findByTicker(ticker)
                .orElseThrow(() -> new RuntimeException("해당 티커를 찾을 수 없습니다: " + ticker));
    }

    /**
     * [수정됨] 버핏 기준 우량주(90점 이상)만 UI에 노출함
     */
    public List<StockOfUsa> getQualifiedStocks() {
        // "Y"인 것만 가져오므로, 아키텍트님이 말씀하신 "False면 안 보여줄 거야"가 실현됨
        return stockOfUsaRepository.findByIsQualifiedOrderByQualityScoreDesc("Y");
    }

    /**
     * [추가] 황금색 카드(강력 추천)만 따로 모아보기 기능
     */
    public List<StockOfUsa> getStrongBuyStocks() {
        // 90점 이상이면서 상태가 STRONG_BUY인 것만 필터링 (나중에 Repo에 추가 가능)
        // Stream을 쓰는 대신 Repository 메서드를 호출하여 성능 향상
        return stockOfUsaRepository.findByIsQualifiedAndPriceStatusOrderByQualityScoreDesc("Y", "STRONG_BUY");
    }
}
