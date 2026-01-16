"""
워렌 버핏 기준 우량주 평가 테스트 스크립트

목적: yfinance로 가져온 데이터로 실제 우량주 평가가 가능한지 테스트
- 6가지 우량주 평가 항목
- 적정가 계산
- EPS 성장률 분석

사용법: python test_buffett_evaluation.py
"""

import yfinance as yf
from curl_cffi.requests import Session
import pandas as pd
from datetime import datetime
import math

# SSL 인증서 에러 우회용 세션 생성
session = Session(impersonate="chrome")
session.verify = False


def calculate_roe(net_income, total_equity):
    """ROE 계산 (Return on Equity)"""
    if total_equity == 0 or pd.isna(total_equity):
        return 0.0
    return (net_income / total_equity) * 100


def calculate_roic(ebit, tax_rate, total_equity, total_liabilities):
    """ROIC 계산 (Return on Invested Capital)"""
    if pd.isna(ebit) or pd.isna(tax_rate):
        return 0.0

    nopat = ebit * (1 - tax_rate / 100)
    invested_capital = total_equity + total_liabilities

    if invested_capital == 0:
        return 0.0

    return (nopat / invested_capital) * 100


def calculate_net_margin(net_income, revenue):
    """Net Margin 계산"""
    if revenue == 0 or pd.isna(revenue):
        return 0.0
    return (net_income / revenue) * 100


def calculate_fcf_margin(free_cash_flow, revenue):
    """FCF Margin 계산"""
    if revenue == 0 or pd.isna(revenue):
        return 0.0
    return (free_cash_flow / revenue) * 100


def calculate_cagr(start_value, end_value, years):
    """CAGR 계산 (연평균 복리 성장률)"""
    if start_value <= 0 or pd.isna(start_value) or pd.isna(end_value):
        return 0.0

    ratio = end_value / start_value
    cagr = (math.pow(ratio, 1.0 / years) - 1) * 100
    return max(cagr, 0.0)


def evaluate_buffett_criteria(ticker):
    """
    워렌 버핏 기준으로 종목 평가

    Args:
        ticker (str): 종목 티커

    Returns:
        dict: 평가 결과
    """
    print(f"\n{'=' * 80}")
    print(f"워렌 버핏 기준 우량주 평가: {ticker}")
    print(f"{'=' * 80}\n")

    try:
        stock = yf.Ticker(ticker, session=session)

        # 필요한 데이터 가져오기
        financials = stock.financials  # 손익계산서
        balance_sheet = stock.balance_sheet  # 재무상태표
        cashflow = stock.cashflow  # 현금흐름표
        info = stock.info

        if financials.empty or balance_sheet.empty or cashflow.empty:
            print("[오류] 재무 데이터를 가져올 수 없습니다.")
            return None

        # 데이터 연도 수 확인
        years_available = len(financials.columns)
        print(f"📊 사용 가능한 데이터: {years_available}년")
        print(f"📅 데이터 기간: {[col.year for col in financials.columns]}\n")

        # ================================================================
        # 데이터 추출 및 계산
        # ================================================================

        results = []

        for i, date in enumerate(financials.columns):
            year = date.year

            # 손익계산서 데이터
            revenue = (
                financials.loc["Total Revenue", date]
                if "Total Revenue" in financials.index
                else 0
            )
            net_income = (
                financials.loc["Net Income", date]
                if "Net Income" in financials.index
                else 0
            )
            ebit = financials.loc["EBIT", date] if "EBIT" in financials.index else 0
            pretax_income = (
                financials.loc["Pretax Income", date]
                if "Pretax Income" in financials.index
                else 0
            )
            tax_provision = (
                financials.loc["Tax Provision", date]
                if "Tax Provision" in financials.index
                else 0
            )
            interest_expense = (
                financials.loc["Interest Expense", date]
                if "Interest Expense" in financials.index
                else 0
            )

            # 재무상태표 데이터
            total_equity = (
                balance_sheet.loc["Stockholders Equity", date]
                if "Stockholders Equity" in balance_sheet.index
                else 0
            )
            total_liabilities = (
                balance_sheet.loc["Total Liabilities Net Minority Interest", date]
                if "Total Liabilities Net Minority Interest" in balance_sheet.index
                else 0
            )

            # 현금흐름표 데이터
            free_cash_flow = (
                cashflow.loc["Free Cash Flow", date]
                if "Free Cash Flow" in cashflow.index
                else 0
            )

            # EPS (주당순이익)
            diluted_eps = (
                financials.loc["Diluted EPS", date]
                if "Diluted EPS" in financials.index
                else 0
            )

            # 세율 계산
            tax_rate = (
                (tax_provision / pretax_income * 100) if pretax_income != 0 else 0
            )

            # 지표 계산
            roe = calculate_roe(net_income, total_equity)
            roic = calculate_roic(ebit, tax_rate, total_equity, total_liabilities)
            net_margin = calculate_net_margin(net_income, revenue)
            fcf_margin = calculate_fcf_margin(free_cash_flow, revenue)
            debt_ratio = (
                (total_liabilities / total_equity * 100) if total_equity != 0 else 0
            )
            interest_coverage = (
                (ebit / abs(interest_expense))
                if interest_expense != 0
                else float("inf")
            )

            results.append(
                {
                    "year": year,
                    "revenue": revenue,
                    "net_income": net_income,
                    "ebit": ebit,
                    "total_equity": total_equity,
                    "total_liabilities": total_liabilities,
                    "free_cash_flow": free_cash_flow,
                    "eps": diluted_eps,
                    "roe": roe,
                    "roic": roic,
                    "net_margin": net_margin,
                    "fcf_margin": fcf_margin,
                    "debt_ratio": debt_ratio,
                    "interest_coverage": interest_coverage,
                    "interest_expense": interest_expense,
                }
            )

        # 최신 데이터가 먼저 오므로 역순으로 정렬 (과거 → 현재)
        results.reverse()

        # ================================================================
        # 데이터 유효성 검증 및 필터링
        # ================================================================
        print("=" * 80)
        print("📋 데이터 유효성 검증")
        print("=" * 80)

        # 불완전한 데이터 제외 (Net Income, Total Equity, Revenue가 모두 있어야 함)
        valid_results = []
        invalid_years = []

        for r in results:
            is_valid = (
                r["net_income"] != 0
                and not pd.isna(r["net_income"])
                and r["total_equity"] != 0
                and not pd.isna(r["total_equity"])
                and r["revenue"] != 0
                and not pd.isna(r["revenue"])
                and not pd.isna(r["eps"])
            )

            if is_valid:
                valid_results.append(r)
                print(f"  ✅ {r['year']}: 유효한 데이터")
            else:
                invalid_years.append(r["year"])
                print(f"  ❌ {r['year']}: 불완전한 데이터 (평가에서 제외)")

        if len(invalid_years) > 0:
            print(f"\n⚠️ 제외된 연도: {invalid_years}")
            print(
                f"⚠️ 이유: 재무제표 데이터 불완전 (Net Income, Equity, Revenue 중 누락)"
            )

        # 유효한 데이터가 너무 적으면 평가 중단
        if len(valid_results) < 3:
            print(
                f"\n❌ 오류: 유효한 데이터가 {len(valid_results)}년뿐입니다. (최소 3년 필요)"
            )
            return None

        # 이후 평가에는 valid_results만 사용
        results = valid_results
        years_available = len(results)

        print(
            f"\n✅ 평가에 사용할 데이터: {years_available}년 ({[r['year'] for r in results]})\n"
        )

        # ================================================================
        # [1] ROE 지속성 평가 (25점)
        # ================================================================
        print("=" * 80)
        print("[1] ROE 지속성 평가 (25점 만점)")
        print("=" * 80)

        count_15_plus = sum(1 for r in results if r["roe"] >= 15.0)
        count_12_plus = sum(1 for r in results if r["roe"] >= 12.0)
        has_loss = any(r["roe"] < 0 for r in results)

        print(f"\n📈 연도별 ROE:")
        for r in results:
            status = "✅" if r["roe"] >= 15.0 else "⚠️" if r["roe"] >= 12.0 else "❌"
            print(f"  {r['year']}: {r['roe']:.2f}% {status}")

        print(f"\n📊 통계:")
        print(f"  - 15% 이상: {count_15_plus}/{years_available}년")
        print(f"  - 12% 이상: {count_12_plus}/{years_available}년")
        print(f"  - 적자 여부: {'있음 ❌' if has_loss else '없음 ✅'}")

        roe_score = 0
        if has_loss:
            roe_score = 0
            print(f"\n🎯 ROE 점수: {roe_score}/25점 (적자로 인한 자동 탈락)")
        elif count_15_plus == years_available:
            roe_score = 25
            print(f"\n🎯 ROE 점수: {roe_score}/25점 (완벽!)")
        elif count_15_plus >= years_available * 0.8:
            roe_score = 20
            print(f"\n🎯 ROE 점수: {roe_score}/25점 (우수)")
        elif count_12_plus == years_available:
            roe_score = 15
            print(f"\n🎯 ROE 점수: {roe_score}/25점 (양호)")
        elif count_12_plus >= years_available * 0.8:
            roe_score = 10
            print(f"\n🎯 ROE 점수: {roe_score}/25점 (보통)")
        else:
            print(f"\n🎯 ROE 점수: {roe_score}/25점 (미흡)")

        # ================================================================
        # [2] ROIC 지속성 평가 (20점)
        # ================================================================
        print("\n" + "=" * 80)
        print("[2] ROIC 지속성 평가 (20점 만점)")
        print("=" * 80)

        count_12_plus_roic = sum(1 for r in results if r["roic"] >= 12.0)
        count_9_plus_roic = sum(1 for r in results if r["roic"] >= 9.0)

        print(f"\n📈 연도별 ROIC:")
        for r in results:
            status = "✅" if r["roic"] >= 12.0 else "⚠️" if r["roic"] >= 9.0 else "❌"
            print(f"  {r['year']}: {r['roic']:.2f}% {status}")

        print(f"\n📊 통계:")
        print(f"  - 12% 이상: {count_12_plus_roic}/{years_available}년")
        print(f"  - 9% 이상: {count_9_plus_roic}/{years_available}년")

        roic_score = 0
        if count_12_plus_roic == years_available:
            roic_score = 20
            print(f"\n🎯 ROIC 점수: {roic_score}/20점 (완벽!)")
        elif count_12_plus_roic >= years_available * 0.8:
            roic_score = 15
            print(f"\n🎯 ROIC 점수: {roic_score}/20점 (우수)")
        elif count_9_plus_roic == years_available:
            roic_score = 10
            print(f"\n🎯 ROIC 점수: {roic_score}/20점 (양호)")
        elif count_9_plus_roic >= years_available * 0.8:
            roic_score = 5
            print(f"\n🎯 ROIC 점수: {roic_score}/20점 (보통)")
        else:
            print(f"\n🎯 ROIC 점수: {roic_score}/20점 (미흡)")

        # ================================================================
        # [3] Net Margin 안정성 평가 (15점)
        # ================================================================
        print("\n" + "=" * 80)
        print("[3] Net Margin 안정성 평가 (15점 만점)")
        print("=" * 80)

        margins = [r["net_margin"] for r in results]
        avg_margin = sum(margins) / len(margins)
        variance = sum((m - avg_margin) ** 2 for m in margins) / len(margins)
        std_dev = math.sqrt(variance)

        print(f"\n📈 연도별 Net Margin:")
        for r in results:
            print(f"  {r['year']}: {r['net_margin']:.2f}%")

        print(f"\n📊 통계:")
        print(f"  - 평균: {avg_margin:.2f}%")
        print(
            f"  - 표준편차: {std_dev:.2f}% {'✅ 안정적' if std_dev <= 5.0 else '⚠️ 변동성 높음'}"
        )

        # 평균 점수 (10점)
        avg_score = 0
        if avg_margin >= 20.0:
            avg_score = 10
        elif avg_margin >= 15.0:
            avg_score = 7
        elif avg_margin >= 10.0:
            avg_score = 5

        # 안정성 점수 (5점)
        stability_score = 0
        if std_dev <= 3.0:
            stability_score = 5
        elif std_dev <= 5.0:
            stability_score = 3
        elif std_dev <= 8.0:
            stability_score = 1

        margin_score = avg_score + stability_score
        print(
            f"\n🎯 Net Margin 점수: {margin_score}/15점 (평균: {avg_score}/10, 안정성: {stability_score}/5)"
        )

        # ================================================================
        # [4] 수익성 추세 평가 (15점)
        # ================================================================
        print("\n" + "=" * 80)
        print("[4] 수익성 추세 평가 (15점 만점)")
        print("=" * 80)

        # 최근 3년과 과거 나머지 년도 비교
        if years_available >= 4:
            recent_years = min(3, years_available - 1)
            past_years = years_available - recent_years

            recent_roe = sum(r["roe"] for r in results[-recent_years:]) / recent_years
            past_roe = sum(r["roe"] for r in results[:past_years]) / past_years

            improvement = (
                ((recent_roe - past_roe) / past_roe * 100) if past_roe != 0 else 0
            )

            print(f"\n📊 ROE 추세 분석:")
            print(f"  - 과거 {past_years}년 평균 ROE: {past_roe:.2f}%")
            print(f"  - 최근 {recent_years}년 평균 ROE: {recent_roe:.2f}%")
            print(f"  - 개선도: {improvement:+.2f}%")

            trend_score = 0
            if improvement >= 20.0:
                trend_score = 15
                print(f"\n🎯 추세 점수: {trend_score}/15점 (급성장! 🚀)")
            elif improvement >= 10.0:
                trend_score = 12
                print(f"\n🎯 추세 점수: {trend_score}/15점 (성장 중 📈)")
            elif improvement >= 5.0:
                trend_score = 9
                print(f"\n🎯 추세 점수: {trend_score}/15점 (완만한 성장)")
            elif improvement >= 0.0:
                trend_score = 6
                print(f"\n🎯 추세 점수: {trend_score}/15점 (유지)")
            elif improvement >= -5.0:
                trend_score = 3
                print(f"\n🎯 추세 점수: {trend_score}/15점 (약간 하락 ⚠️)")
            else:
                print(f"\n🎯 추세 점수: {trend_score}/15점 (하락 추세 ❌)")
        else:
            trend_score = 0
            print(f"\n⚠️ 데이터 부족 (최소 4년 필요)")

        # ================================================================
        # [5] 재무 건전성 평가 (15점)
        # ================================================================
        print("\n" + "=" * 80)
        print("[5] 재무 건전성 평가 (15점 만점)")
        print("=" * 80)

        latest = results[-1]

        print(f"\n📊 최근 연도 ({latest['year']}) 재무 건전성:")
        print(f"  - 부채비율: {latest['debt_ratio']:.2f}%")
        print(
            f"  - 이자보상배율: {latest['interest_coverage']:.2f}배"
            if latest["interest_coverage"] != float("inf")
            else "  - 이자보상배율: 무차입 경영 ✅"
        )

        # 부채비율 점수 (10점)
        debt_score = 0
        if latest["debt_ratio"] <= 50.0:
            debt_score = 10
        elif latest["debt_ratio"] <= 80.0:
            debt_score = 7
        elif latest["debt_ratio"] <= 120.0:
            debt_score = 4
        elif latest["debt_ratio"] <= 150.0:
            debt_score = 2

        # 이자보상배율 점수 (5점)
        coverage_score = 0
        if latest["interest_expense"] == 0 or pd.isna(latest["interest_expense"]):
            coverage_score = 5  # 무차입
        elif latest["interest_coverage"] != float("inf") and not pd.isna(
            latest["interest_coverage"]
        ):
            if latest["interest_coverage"] >= 10.0:
                coverage_score = 5
            elif latest["interest_coverage"] >= 5.0:
                coverage_score = 3
            elif latest["interest_coverage"] >= 3.0:
                coverage_score = 1

        health_score = debt_score + coverage_score
        print(
            f"\n🎯 재무 건전성 점수: {health_score}/15점 (부채: {debt_score}/10, 이자보상: {coverage_score}/5)"
        )

        # ================================================================
        # [6] 현금창출력 평가 (10점)
        # ================================================================
        print("\n" + "=" * 80)
        print("[6] 현금창출력 평가 (10점 만점)")
        print("=" * 80)

        fcf_margins = [r["fcf_margin"] for r in results]
        avg_fcf_margin = sum(fcf_margins) / len(fcf_margins)

        print(f"\n📈 연도별 FCF Margin:")
        for r in results:
            print(f"  {r['year']}: {r['fcf_margin']:.2f}%")

        print(f"\n📊 {years_available}년 평균 FCF Margin: {avg_fcf_margin:.2f}%")

        cash_score = 0
        if avg_fcf_margin >= 15.0:
            cash_score = 10
            print(f"\n🎯 현금창출력 점수: {cash_score}/10점 (우수! 💰)")
        elif avg_fcf_margin >= 10.0:
            cash_score = 7
            print(f"\n🎯 현금창출력 점수: {cash_score}/10점 (양호)")
        elif avg_fcf_margin >= 5.0:
            cash_score = 4
            print(f"\n🎯 현금창출력 점수: {cash_score}/10점 (보통)")
        elif avg_fcf_margin >= 0.0:
            cash_score = 2
            print(f"\n🎯 현금창출력 점수: {cash_score}/10점 (미흡)")
        else:
            print(f"\n🎯 현금창출력 점수: {cash_score}/10점 (부족 ❌)")

        # ================================================================
        # 우량주 종합 점수
        # ================================================================
        total_score = (
            roe_score
            + roic_score
            + margin_score
            + trend_score
            + health_score
            + cash_score
        )

        print("\n" + "=" * 80)
        print("🏆 우량주 종합 평가")
        print("=" * 80)
        print(f"\n점수 상세:")
        print(f"  1. ROE 지속성:      {roe_score:2d}/25점")
        print(f"  2. ROIC 지속성:     {roic_score:2d}/20점")
        print(f"  3. Net Margin 안정: {margin_score:2d}/15점")
        print(f"  4. 수익성 추세:     {trend_score:2d}/15점")
        print(f"  5. 재무 건전성:     {health_score:2d}/15점")
        print(f"  6. 현금창출력:      {cash_score:2d}/10점")
        print(f"  " + "-" * 40)
        print(f"  총점:              {total_score:2d}/100점")

        if total_score >= 85:
            print(f"\n✅ 결과: 우량주 통과! (85점 이상)")
        else:
            print(f"\n❌ 결과: 우량주 기준 미달 (85점 미만)")

        # ================================================================
        # [7] 적정가 계산
        # ================================================================
        print("\n" + "=" * 80)
        print("💰 적정가(내재가치) 계산")
        print("=" * 80)

        # EPS 데이터
        eps_list = [r["eps"] for r in results]
        oldest_eps = eps_list[0]
        latest_eps = eps_list[-1]

        print(f"\n📊 EPS 분석:")
        for r in results:
            print(f"  {r['year']}: ${r['eps']:.2f}")

        # EPS 성장률 계산 (CAGR)
        eps_cagr = calculate_cagr(oldest_eps, latest_eps, years_available - 1)
        print(f"\n📈 EPS {years_available - 1}년 CAGR: {eps_cagr:.2f}%")

        # 미래 EPS 추정 (5년 후, 보수적으로 70%만 반영)
        conservative_growth = eps_cagr * 0.7
        future_eps = latest_eps * math.pow(1 + conservative_growth / 100, 5)

        print(
            f"🔮 5년 후 예상 EPS: ${future_eps:.2f} (보수적 성장률 {conservative_growth:.2f}% 적용)"
        )

        # 적정 PER 결정
        if eps_cagr >= 15.0:
            fair_per = 18.0
        elif eps_cagr >= 8.0:
            fair_per = 12.0
        elif eps_cagr >= 0.0:
            fair_per = 10.0
        else:
            fair_per = 8.0

        print(f"📐 적정 PER: {fair_per} (성장률 기반)")

        # 이론적 적정가
        theoretical_value = future_eps * fair_per
        print(f"💵 이론적 적정가: ${theoretical_value:.2f}")

        # 안전마진 20% 적용
        intrinsic_value = theoretical_value * 0.8
        print(f"🎯 최종 적정가: ${intrinsic_value:.2f} (안전마진 20% 적용)")

        # 현재가와 비교
        current_price = info.get("currentPrice", 0)
        print(f"\n💰 현재가: ${current_price:.2f}")

        if current_price <= intrinsic_value:
            upside = (intrinsic_value - current_price) / current_price * 100
            print(f"✅ 저평가 구간! (상승여력: {upside:.2f}%) 🚀")
            print(f"   → STRONG_BUY 추천!")
        else:
            downside = (current_price - intrinsic_value) / current_price * 100
            print(f"⚠️ 고평가 구간 (하락위험: {downside:.2f}%)")
            print(f"   → 목표가 ${intrinsic_value:.2f}까지 대기 권장")

        print("\n" + "=" * 80)
        print("평가 완료!")
        print("=" * 80 + "\n")

        return {
            "total_score": total_score,
            "intrinsic_value": intrinsic_value,
            "current_price": current_price,
        }

    except Exception as e:
        print(f"\n[오류] 평가 중 오류 발생: {str(e)}")
        import traceback

        traceback.print_exc()
        return None


def main():
    """메인 실행 함수"""
    print("\n" + "=" * 80)
    print("워렌 버핏 기준 우량주 평가 시스템 테스트")
    print("=" * 80)
    print(f"\n⏰ 테스트 시작: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

    # AAPL 평가
    result = evaluate_buffett_criteria("AAPL")

    if result:
        print("\n✅ 테스트 성공!")
    else:
        print("\n❌ 테스트 실패!")

    print(f"\n⏰ 테스트 종료: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")


if __name__ == "__main__":
    main()
