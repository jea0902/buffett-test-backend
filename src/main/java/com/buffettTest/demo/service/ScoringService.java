package com.buffettTest.demo.service;

import com.buffettTest.demo.entity.FinancialAnnual;
import com.buffettTest.demo.entity.StockOfUsa;
import com.buffettTest.demo.entity.StockPrice;
import com.buffettTest.demo.repository.FinancialAnnualRepository;
import com.buffettTest.demo.repository.StockOfUsaRepository;
import com.buffettTest.demo.repository.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 워렌 버핏 기준 우량주 평가 및 적정가 계산 서비스
 * 
 * [처리 흐름]
 * 1. 우량주 평가 (Quality Scoring) - 85점 이상만 통과
 * 2. 적정가 계산 (Intrinsic Value Calculation) - 우량주 통과자만
 * 3. 저평가 판단 (Price Status) - 현재가 vs 적정가 비교
 * 
 * [버핏의 핵심 철학 반영]
 * - 지속성: 평균이 아닌 일관성을 평가
 * - 안정성: 변동성이 낮은 기업 선호
 * - 추세: 과거보다 현재가 더 좋아야 함
 * - 보수성: 의심스러우면 탈락
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoringService {

    private final StockOfUsaRepository stockOfUsaRepository;
    private final FinancialAnnualRepository financialAnnualRepository;
    private final StockPriceRepository stockPriceRepository;

    // ===================================================================
    // 메인 진입점: 종목의 우량도 평가 + 적정가 계산 + 저평가 판단
    // ===================================================================

    @Transactional
    public void calculateAndSaveScore(Long stockId) {
        log.info("=== 종목 평가 시작: stockId={} ===", stockId);

        // [1] 필요한 데이터 모두 가져오기
        StockOfUsa stock = stockOfUsaRepository.findById(stockId)
                .orElseThrow(() -> new RuntimeException("종목을 찾을 수 없습니다: " + stockId));

        List<FinancialAnnual> financials = financialAnnualRepository
                .findByStock_StockIdOrderByFiscalYearDesc(stockId);

        StockPrice priceData = stockPriceRepository.findById(stockId)
                .orElseThrow(() -> new RuntimeException("가격 정보를 찾을 수 없습니다: " + stockId));

        // [2] 1단계: 우량주 평가 (85점 미만은 여기서 탈락)
        BigDecimal qualityScore = evaluateQualityScore(financials);
        stock.setQualityScore(qualityScore);

        if (qualityScore.compareTo(new BigDecimal("85")) < 0) {
            stock.setIsQualified("N");
            stock.setPriceStatus(null);
            stock.setTargetPrice(null);
            log.info("우량주 기준 미달: {}점 → 평가 종료", qualityScore);
            return; // 85점 미만은 더 이상 계산 안 함
        }

        stock.setIsQualified("Y");
        log.info("우량주 통과: {}점", qualityScore);

        // [3] 2단계: 적정가 계산 (우량주만)
        BigDecimal intrinsicValue = calculateIntrinsicValue(stock, financials);
        log.info("적정가 계산 완료: ${}", intrinsicValue);

        // [4] 3단계: 저평가 판단 (현재가 vs 적정가)
        evaluatePriceStatus(stock, priceData.getCurrentPrice(), intrinsicValue);

        log.info("=== 종목 평가 완료: {} ===", stock.getPriceStatus());
    }

    // ===================================================================
    // [1단계] 우량주 평가: 버핏 철학 완전 반영 (6가지 항목)
    // ===================================================================

    /**
     * 워렌 버핏 기준 우량주 평가 (완전판)
     * 
     * 평가 항목 (총 100점):
     * 1. ROE 지속성 (25점) - 10년 중 몇 년이 기준 충족했는지
     * 2. ROIC 지속성 (20점) - 10년 중 몇 년이 기준 충족했는지
     * 3. Net Margin 안정성 (15점) - 변동성이 낮을수록 높은 점수
     * 4. 수익성 추세 (15점) - 최근이 과거보다 좋아야 함
     * 5. 재무 건전성 (15점) - 부채비율 + 이자보상배율
     * 6. 현금창출력 (10점) - FCF Margin 평가
     * 
     * @return 0~100점 사이의 우량도 점수
     */
    private BigDecimal evaluateQualityScore(List<FinancialAnnual> financials) {
        // 데이터 검증: 최소 10년치 필요
        if (financials == null || financials.size() < 10) {
            log.warn("재무 데이터 부족: {}년 (최소 10년 필요)",
                    financials != null ? financials.size() : 0);
            return BigDecimal.ZERO;
        }

        BigDecimal totalScore = BigDecimal.ZERO;

        // [1] ROE 지속성 평가 - 25점
        BigDecimal roeScore = evaluateROEConsistency(financials);
        totalScore = totalScore.add(roeScore);
        log.debug("ROE 지속성 점수: {}/25점", roeScore);

        // [2] ROIC 지속성 평가 - 20점
        BigDecimal roicScore = evaluateROICConsistency(financials);
        totalScore = totalScore.add(roicScore);
        log.debug("ROIC 지속성 점수: {}/20점", roicScore);

        // [3] Net Margin 안정성 평가 - 15점
        BigDecimal marginScore = evaluateMarginStability(financials);
        totalScore = totalScore.add(marginScore);
        log.debug("Net Margin 안정성 점수: {}/15점", marginScore);

        // [4] 수익성 추세 평가 - 15점
        BigDecimal trendScore = evaluateProfitabilityTrend(financials);
        totalScore = totalScore.add(trendScore);
        log.debug("수익성 추세 점수: {}/15점", trendScore);

        // [5] 재무 건전성 평가 - 15점
        BigDecimal healthScore = evaluateFinancialHealth(financials);
        totalScore = totalScore.add(healthScore);
        log.debug("재무 건전성 점수: {}/15점", healthScore);

        // [6] 현금창출력 평가 - 10점
        BigDecimal cashScore = evaluateCashGenerationPower(financials);
        totalScore = totalScore.add(cashScore);
        log.debug("현금창출력 점수: {}/10점", cashScore);

        log.info("우량도 최종 점수: {}점", totalScore);

        return totalScore.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * [1] ROE 지속성 평가 (25점 만점)
     * 
     * 버핏: "10년 내내 15% 이상 ROE를 유지하는 기업은 극소수"
     * 
     * 평가 기준:
     * - 10년 모두 15% 이상: 25점
     * - 8년 이상 15% 이상: 20점
     * - 10년 모두 12% 이상: 15점
     * - 8년 이상 12% 이상: 10점
     * - 그 외: 0점
     * - 적자 1회라도 있으면: 자동 탈락 (0점)
     */
    private BigDecimal evaluateROEConsistency(List<FinancialAnnual> financials) {
        int count15Plus = 0;
        int count12Plus = 0;
        boolean hasLoss = false;

        for (FinancialAnnual f : financials) {
            double roe = f.getRoePct().doubleValue();

            if (roe < 0) {
                hasLoss = true;
                log.warn("ROE 적자 발견: {}년 {}%", f.getFiscalYear(), roe);
                break;
            }

            if (roe >= 15.0)
                count15Plus++;
            if (roe >= 12.0)
                count12Plus++;
        }

        // 적자가 한 번이라도 있으면 0점
        if (hasLoss) {
            log.warn("ROE 적자로 인한 자동 탈락");
            return BigDecimal.ZERO;
        }

        // 지속성 평가
        if (count15Plus == 10)
            return new BigDecimal("25");
        if (count15Plus >= 8)
            return new BigDecimal("20");
        if (count12Plus == 10)
            return new BigDecimal("15");
        if (count12Plus >= 8)
            return new BigDecimal("10");

        return BigDecimal.ZERO;
    }

    /**
     * [2] ROIC 지속성 평가 (20점 만점)
     * 
     * 버핏: "자본을 효율적으로 사용하는 기업이 장기적으로 승리한다"
     * 
     * 평가 기준:
     * - 10년 모두 12% 이상: 20점
     * - 8년 이상 12% 이상: 15점
     * - 10년 모두 9% 이상: 10점
     * - 8년 이상 9% 이상: 5점
     * - 그 외: 0점
     */
    private BigDecimal evaluateROICConsistency(List<FinancialAnnual> financials) {
        int count12Plus = 0;
        int count9Plus = 0;

        for (FinancialAnnual f : financials) {
            double roic = f.getRoicPct().doubleValue();

            if (roic >= 12.0)
                count12Plus++;
            if (roic >= 9.0)
                count9Plus++;
        }

        if (count12Plus == 10)
            return new BigDecimal("20");
        if (count12Plus >= 8)
            return new BigDecimal("15");
        if (count9Plus == 10)
            return new BigDecimal("10");
        if (count9Plus >= 8)
            return new BigDecimal("5");

        return BigDecimal.ZERO;
    }

    /**
     * [3] Net Margin 안정성 평가 (15점 만점)
     * 
     * 버핏: "예측 가능한 수익성이 중요하다"
     * 
     * 평가 기준:
     * - 10년 평균 Margin과 표준편차를 함께 평가
     * - 평균이 높고 변동성이 낮을수록 높은 점수
     */
    private BigDecimal evaluateMarginStability(List<FinancialAnnual> financials) {
        double[] margins = financials.stream()
                .mapToDouble(f -> f.getNetMarginPct().doubleValue())
                .toArray();

        double avgMargin = average(margins);
        double stdDev = standardDeviation(margins);

        log.debug("Net Margin - 평균: {:.2f}%, 표준편차: {:.2f}", avgMargin, stdDev);

        // 평균 점수 (10점)
        BigDecimal avgScore = BigDecimal.ZERO;
        if (avgMargin >= 20.0)
            avgScore = new BigDecimal("10");
        else if (avgMargin >= 15.0)
            avgScore = new BigDecimal("7");
        else if (avgMargin >= 10.0)
            avgScore = new BigDecimal("5");

        // 안정성 점수 (5점) - 표준편차가 낮을수록 좋음
        BigDecimal stabilityScore = BigDecimal.ZERO;
        if (stdDev <= 3.0)
            stabilityScore = new BigDecimal("5");
        else if (stdDev <= 5.0)
            stabilityScore = new BigDecimal("3");
        else if (stdDev <= 8.0)
            stabilityScore = new BigDecimal("1");

        return avgScore.add(stabilityScore);
    }

    /**
     * [4] 수익성 추세 평가 (15점 만점)
     * 
     * 버핏: "과거보다 현재가 더 좋은 기업에 투자하라"
     * 
     * 평가 기준:
     * - 최근 3년 평균 ROE vs 과거 7년 평균 ROE 비교
     * - 상승 추세일수록 높은 점수
     */
    private BigDecimal evaluateProfitabilityTrend(List<FinancialAnnual> financials) {
        // 최근 3년 평균 ROE
        double recent3YearRoe = financials.subList(0, 3).stream()
                .mapToDouble(f -> f.getRoePct().doubleValue())
                .average()
                .orElse(0.0);

        // 과거 7년 평균 ROE
        double past7YearRoe = financials.subList(3, 10).stream()
                .mapToDouble(f -> f.getRoePct().doubleValue())
                .average()
                .orElse(0.0);

        double improvement = ((recent3YearRoe - past7YearRoe) / past7YearRoe) * 100;

        log.debug("ROE 추세 - 최근 3년: {:.2f}%, 과거 7년: {:.2f}%, 개선도: {:.2f}%",
                recent3YearRoe, past7YearRoe, improvement);

        // 개선도에 따른 점수
        if (improvement >= 20.0)
            return new BigDecimal("15"); // 20% 이상 개선
        if (improvement >= 10.0)
            return new BigDecimal("12"); // 10% 이상 개선
        if (improvement >= 5.0)
            return new BigDecimal("9"); // 5% 이상 개선
        if (improvement >= 0.0)
            return new BigDecimal("6"); // 유지
        if (improvement >= -5.0)
            return new BigDecimal("3"); // 약간 하락

        return BigDecimal.ZERO; // 5% 이상 하락은 0점
    }

    /**
     * [5] 재무 건전성 평가 (15점 만점)
     * 
     * 버핏: "부채가 적고 이자를 쉽게 갚을 수 있는 기업"
     * 
     * 평가 기준:
     * - 부채비율 (10점)
     * - 이자보상배율 (5점)
     */
    private BigDecimal evaluateFinancialHealth(List<FinancialAnnual> financials) {
        FinancialAnnual latest = financials.get(0);

        // [1] 부채비율 평가 (10점)
        BigDecimal totalLiabilities = new BigDecimal(latest.getTotalLiabilities());
        BigDecimal totalEquity = new BigDecimal(latest.getTotalEquity());

        double debtRatio = totalLiabilities
                .divide(totalEquity, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();

        BigDecimal debtScore = BigDecimal.ZERO;
        if (debtRatio <= 50.0)
            debtScore = new BigDecimal("10");
        else if (debtRatio <= 80.0)
            debtScore = new BigDecimal("7");
        else if (debtRatio <= 120.0)
            debtScore = new BigDecimal("4");
        else if (debtRatio <= 150.0)
            debtScore = new BigDecimal("2");

        log.debug("부채비율: {:.2f}% → {}점", debtRatio, debtScore);

        // [2] 이자보상배율 평가 (5점)
        // 이자보상배율 = EBIT / 이자비용 (높을수록 좋음)
        BigDecimal interestCoverageScore = BigDecimal.ZERO;

        // Long → BigDecimal 변환
        BigDecimal ebit = new BigDecimal(latest.getEbit());
        BigDecimal interestExpense = new BigDecimal(latest.getInterestExpense());

        if (interestExpense.compareTo(BigDecimal.ZERO) > 0) {
            double coverageRatio = ebit
                    .divide(interestExpense, 4, RoundingMode.HALF_UP)
                    .doubleValue();

            if (coverageRatio >= 10.0)
                interestCoverageScore = new BigDecimal("5");
            else if (coverageRatio >= 5.0)
                interestCoverageScore = new BigDecimal("3");
            else if (coverageRatio >= 3.0)
                interestCoverageScore = new BigDecimal("1");

            log.debug("이자보상배율: {:.2f}배 → {}점", coverageRatio, interestCoverageScore);
        } else {
            // 이자비용이 없으면 만점 (무차입 경영)
            interestCoverageScore = new BigDecimal("5");
            log.debug("무차입 경영 → 5점");
        }

        return debtScore.add(interestCoverageScore);
    }

    /**
     * [6] 현금창출력 평가 (10점 만점)
     * 
     * 버핏: "회계상 이익보다 실제 현금흐름이 중요하다"
     * 
     * 평가 기준:
     * - FCF Margin = FCF / Revenue (높을수록 좋음)
     * - 10년 평균 평가
     */
    private BigDecimal evaluateCashGenerationPower(List<FinancialAnnual> financials) {
        double avgFcfMargin = financials.stream()
                .mapToDouble(f -> {
                    // Long → BigDecimal 변환
                    BigDecimal revenue = new BigDecimal(f.getRevenue());
                    BigDecimal fcf = new BigDecimal(f.getFreeCashFlow());

                    if (revenue.compareTo(BigDecimal.ZERO) == 0)
                        return 0.0;
                    return fcf.divide(revenue, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                            .doubleValue();
                })
                .average()
                .orElse(0.0);

        log.debug("FCF Margin 평균: {:.2f}%", avgFcfMargin);

        if (avgFcfMargin >= 15.0)
            return new BigDecimal("10");
        if (avgFcfMargin >= 10.0)
            return new BigDecimal("7");
        if (avgFcfMargin >= 5.0)
            return new BigDecimal("4");
        if (avgFcfMargin >= 0.0)
            return new BigDecimal("2");

        return BigDecimal.ZERO;
    }

    // ===================================================================
    // [2단계] 적정가 계산 (워렌 버핏의 내재가치 평가)
    // ===================================================================

    /**
     * 워렌 버핏 기준 적정가(내재가치) 계산
     * 
     * [계산 방법]
     * 1. 10년 평균 EPS 성장률 계산 (CAGR)
     * 2. 미래 EPS 추정 (5년 후 보수적 추정)
     * 3. 적정 PER 결정 (성장률 × 0.8 ~ 1.2 범위)
     * 4. 이론적 적정가 = 미래 EPS × 적정 PER
     * 5. 안전마진 20% 적용 (버핏의 핵심 원칙)
     * 
     * @return 안전마진이 적용된 적정가
     */
    private BigDecimal calculateIntrinsicValue(StockOfUsa stock, List<FinancialAnnual> financials) {
        log.info("=== 적정가 계산 시작 ===");

        // [1] 10년 EPS 데이터 준비 (과거 → 현재 순으로 정렬)
        List<BigDecimal> epsList = financials.stream()
                .sorted((a, b) -> a.getFiscalYear().compareTo(b.getFiscalYear())) // 오름차순
                .map(FinancialAnnual::getEps)
                .toList();

        BigDecimal oldestEps = epsList.get(0); // 10년 전 EPS
        BigDecimal latestEps = epsList.get(epsList.size() - 1); // 최근 EPS

        log.debug("10년 전 EPS: ${}, 최근 EPS: ${}", oldestEps, latestEps);

        // [2] 10년 평균 성장률 계산 (CAGR: Compound Annual Growth Rate)
        double epsGrowthRate = calculateCAGR(oldestEps, latestEps, 10);
        log.info("EPS 10년 평균 성장률: {:.2f}%", epsGrowthRate);

        // [3] 미래 EPS 추정 (5년 후, 보수적으로 성장률 70%만 반영)
        double conservativeGrowthRate = epsGrowthRate * 0.7; // 보수적 접근
        BigDecimal futureEps = latestEps.multiply(
                BigDecimal.valueOf(Math.pow(1 + conservativeGrowthRate / 100, 5))).setScale(2, RoundingMode.HALF_UP);

        log.info("5년 후 예상 EPS: ${} (보수적 성장률 {:.2f}% 적용)",
                futureEps, conservativeGrowthRate);

        // [4] 적정 PER 결정 (성장률 기반, 버핏은 보수적으로 평가)
        BigDecimal fairPer = determineFairPER(epsGrowthRate);
        log.info("적정 PER: {}", fairPer);

        // [5] 이론적 적정가 계산
        BigDecimal theoreticalValue = futureEps.multiply(fairPer)
                .setScale(2, RoundingMode.HALF_UP);

        // [6] 안전마진 20% 적용 (버핏: "좋은 가격에 사는 것이 핵심")
        BigDecimal intrinsicValue = theoreticalValue
                .multiply(new BigDecimal("0.80")) // 20% 할인
                .setScale(2, RoundingMode.HALF_UP);

        log.info("이론적 적정가: ${} → 안전마진 적용 후: ${}",
                theoreticalValue, intrinsicValue);
        log.info("=== 적정가 계산 완료 ===");

        return intrinsicValue;
    }

    /**
     * CAGR (연평균 복리 성장률) 계산
     * 
     * 공식: CAGR = [(최종값 / 초기값)^(1/기간) - 1] × 100
     */
    private double calculateCAGR(BigDecimal startValue, BigDecimal endValue, int years) {
        if (startValue.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("초기 EPS가 0 이하: {} → 성장률 0% 반환", startValue);
            return 0.0;
        }

        double ratio = endValue.divide(startValue, 4, RoundingMode.HALF_UP).doubleValue();
        double cagr = (Math.pow(ratio, 1.0 / years) - 1) * 100;

        return Math.max(cagr, 0.0); // 음수 성장률은 0으로 처리
    }

    /**
     * 적정 PER 결정 (워렌 버핏의 보수적 접근)
     * 
     * 기준:
     * - 고성장 (15% 이상): PER 18 (성장률 × 1.2)
     * - 중성장 (8~15%): PER 12 (성장률 × 1.0)
     * - 저성장 (8% 미만): PER 10 (성장률 × 0.8)
     * - 마이너스 성장: PER 8 (최소값)
     */
    private BigDecimal determineFairPER(double growthRate) {
        if (growthRate >= 15.0) {
            return new BigDecimal("18.0");
        } else if (growthRate >= 8.0) {
            return new BigDecimal("12.0");
        } else if (growthRate >= 0.0) {
            return new BigDecimal("10.0");
        } else {
            return new BigDecimal("8.0");
        }
    }

    // ===================================================================
    // [3단계] 저평가 판단 (현재가 vs 적정가)
    // ===================================================================

    /**
     * 현재가와 적정가를 비교하여 투자 상태 결정
     * 
     * - STRONG_BUY (황금 카드): 현재가 <= 적정가
     * - OVERVALUED (빨간 카드): 현재가 > 적정가 (목표가 제시)
     */
    private void evaluatePriceStatus(StockOfUsa stock, BigDecimal currentPrice, BigDecimal intrinsicValue) {
        if (currentPrice.compareTo(intrinsicValue) <= 0) {
            // 저평가 구간: 지금 바로 사도 좋음
            stock.setPriceStatus("STRONG_BUY");
            stock.setTargetPrice(null); // 이미 싸니까 목표가 불필요
            log.info("저평가 판정: 현재가 ${} <= 적정가 ${}", currentPrice, intrinsicValue);
        } else {
            // 고평가 구간: 적정가까지 하락 대기
            stock.setPriceStatus("OVERVALUED");
            stock.setTargetPrice(intrinsicValue);
            log.info("고평가 판정: 현재가 ${} > 적정가 ${} (목표가 설정)",
                    currentPrice, intrinsicValue);
        }
    }

    // ===================================================================
    // 유틸리티 메서드: 통계 계산
    // ===================================================================

    /**
     * 평균 계산
     */
    private double average(double[] values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    /**
     * 표준편차 계산 (모집단)
     */
    private double standardDeviation(double[] values) {
        double avg = average(values);
        double sum = 0.0;
        for (double value : values) {
            sum += Math.pow(value - avg, 2);
        }
        return Math.sqrt(sum / values.length);
    }
}