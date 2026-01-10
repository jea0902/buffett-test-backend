package com.buffettTest.demo.service;

import com.buffettTest.demo.entity.StockOfUsa;
import com.buffettTest.demo.entity.StockPrice;
import com.buffettTest.demo.repository.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

// 이 클래스는 '데이터 수집 및 외부 연동' 역할을 수행함
// - 외부(트레이딩뷰 등)에서 데이터를 가져와 우리 DB 테이블(FinancialAnnual, StockPrice, StockOfUsa) 형식에 맞게 담아줌
// **0110 따라서 임시 코드

@Service
@RequiredArgsConstructor
public class DataCollectionService {

    private final StockPriceRepository stockPriceRepository;
    // 나중에 FinancialAnnualRepository도 추가될 예정입니다.

    /**
     * 외부 API(트레이딩뷰 등)로부터 실시간 가격 정보를 수집하여 DB에 업데이트함
     * 
     * @param stock : 업데이트할 대상 주식 객체
     */
    @Transactional // DB 값을 바꿔야 하므로 쓰기 권한 부여
    public void collectCurrentPrice(StockOfUsa stock) {
        // 1. 실제로는 여기서 트레이딩뷰 API를 호출하여 데이터를 받아옵니다. (지금은 가상의 데이터)
        BigDecimal fetchedPrice = new BigDecimal("185.92");
        BigDecimal fetchedPer = new BigDecimal("28.5");

        // 2. 이 주식의 기존 가격 정보가 있는지 확인합니다. (1:1 관계이므로 ID가 주식ID와 같음)
        // .orElse()는 데이터가 없으면 새로 만들라는 뜻입니다.
        StockPrice price = stockPriceRepository.findById(stock.getStockId())
                .orElse(StockPrice.builder()
                        .stock(stock) // 데이터가 없으면 새 객체 생성 시 주식 연결
                        .build());

        // 3. 받아온 데이터로 업데이트 (JPA의 '더티 체킹' 기능 덕분에 수정만 하면 자동으로 DB에 반영됨)
        // price.updateData(...) 같은 메서드를 엔티티에 만들어 쓰면 아키텍처가 더 깔끔해집니다.
        // 현재는 이해를 돕기 위해 흐름만 보여드립니다.
        System.out.println(stock.getTicker() + "의 정보를 최신으로 업데이트합니다.");
    }
}