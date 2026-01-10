package com.buffettTest.demo.service;

import com.buffettTest.demo.entity.FinancialAnnual;
import com.buffettTest.demo.entity.StockOfUsa;
import com.buffettTest.demo.entity.StockPrice;
import com.buffettTest.demo.repository.FinancialAnnualRepository;
import com.buffettTest.demo.repository.StockOfUsaRepository;
import com.buffettTest.demo.repository.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// **0110 따라서 임시 코드

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 모드 (성능 최적화)
public class ScoringService {

    private final StockOfUsaRepository stockOfUsaRepository;
    private final FinancialAnnualRepository financialAnnualRepository;
    private final StockPriceRepository stockPriceRepository;

    /**
     * 아키텍트님의 2단계 논리에 따라 주식을 평가하고 결과를 저장함
     */
    @Transactional // 계산 후 데이터를 변경해야 하므로 쓰기 권한 부여
    public void calculateAndSaveScore(Long stockId) {

        // [1단계 재료 준비] DB에서 필요한 모든 데이터를 가져옵니다.
        StockOfUsa stock = stockOfUsaRepository.findById(stockId)
                .orElseThrow(() -> new RuntimeException("주식 정보를 찾을 수 없습니다."));

        List<FinancialAnnual> financials = financialAnnualRepository.findByStock_StockIdOrderByFiscalYearDesc(stockId);

        StockPrice priceData = stockPriceRepository.findById(stockId)
                .orElseThrow(() -> new RuntimeException("가격 정보를 찾을 수 없습니다."));

        // -------------------------------------------------------------------
        // [2단계: 우량도 평가] 10년치 재무 데이터를 요리해서 '우량도 점수'를 뽑습니다.
        // -------------------------------------------------------------------
        // (지금은 임시로 95점을 주지만, 나중에 여기에 ROE 평균 계산 로직이 들어갑니다.)
        BigDecimal qualityScore = new BigDecimal("95.00");

        // 아키텍트님 규칙: 90점 미만은 아예 노출 안 함(False)
        if (qualityScore.compareTo(new BigDecimal("90")) < 0) {
            stock.setIsQualified("N"); // UI 노출 제외
            // 더 이상 계산할 필요가 없으므로 여기서 종료
            return;
        }

        // 90점 이상이면 우량주로 인정(True)하고 UI 노출 설정
        stock.setQualityScore(qualityScore);
        stock.setIsQualified("Y");

        // -------------------------------------------------------------------
        // [3단계: 가격 평가] 우량주 통과자들에 한해 저평가 여부를 판단합니다.
        // -------------------------------------------------------------------
        // (적정가 계산 로직도 나중에 공식화할 예정, 지금은 임시로 $150 설정)
        BigDecimal intrinsicValue = new BigDecimal("150.00");
        BigDecimal currentPrice = priceData.getCurrentPrice();

        // 아키텍트님 규칙: 현재가가 적정가(내재가치)보다 낮으면 '황금색 카드'
        if (currentPrice.compareTo(intrinsicValue) <= 0) {
            stock.setPriceStatus("STRONG_BUY"); // 강력 추천 (황금색)
            stock.setTargetPrice(null); // 이미 싸니까 목표가 필요 없음
        }
        // 아키텍트님 규칙: 현재가가 비싸면 '파란색 카드' + 목표가 제시
        else {
            stock.setPriceStatus("OVERVALUED"); // 고평가 (파란색)
            stock.setTargetPrice(intrinsicValue); // "이 가격까지 떨어지면 사라"고 알려줌
        }

        // [참고] JPA의 '더티 체킹' 기능 덕분에 따로 save()를 호출하지 않아도
        // 메서드가 끝날 때 자동으로 DB에 UPDATE 쿼리가 날아갑니다.
    }
}