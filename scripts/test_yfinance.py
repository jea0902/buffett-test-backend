"""
yfinance 데이터 수집 테스트 스크립트

목적: yfinance 라이브러리가 정상적으로 데이터를 가져오는지 테스트
사용법: python test_yfinance.py
"""

import yfinance as yf
from curl_cffi.requests import Session
# yfinance가 원하는 라이브러리를 사용하되, 거기서 직접 인증서 검증을 꺼버리는 방식 사용

import pandas as pd
from datetime import datetime

# 1. yfinance가 요구하는 전용 세션 생성
# impersonate="chrome"을 넣어 일반 브라우저처럼 위장합니다.
session = Session(impersonate="chrome")

# 2. 이 세션에서 SSL 검증(verify)을 강제로 끕니다.
# 한글 경로 인증서 에러를 우회하는 핵심입니다.
session.verify = False

# 3. 세션을 적용하여 데이터 수집
msft = yf.Ticker("MSFT", session=session)


def test_stock_data(ticker):
    """
    특정 종목의 데이터를 수집하여 출력

    Args:
        ticker (str): 종목 티커 (예: 'AAPL', 'MSFT')
    """
    print(f"\n{'=' * 60}")
    print(f"종목 데이터 수집 테스트: {ticker}")
    print(f"{'=' * 60}\n")

    try:
        # yfinance Ticker 객체 생성
        stock = yf.Ticker(ticker, session=session)

        # 연간 vs 분기별 데이터 개수 비교
        print("[데이터 가용성 비교]")
        print("-" * 60)
        annual_financials = stock.financials
        quarterly_financials = stock.quarterly_financials

        print(
            f"연간 재무제표 개수: {len(annual_financials.columns)}개 (약 {len(annual_financials.columns)}년)"
        )
        print(
            f"분기별 재무제표 개수: {len(quarterly_financials.columns)}개 (약 {len(quarterly_financials.columns) // 4}년)"
        )

        if len(annual_financials.columns) > 0:
            years = [col.year for col in annual_financials.columns]
            print(f"연간 데이터 연도: {min(years)} ~ {max(years)}")

        if len(quarterly_financials.columns) > 0:
            quarters = [
                col.strftime("%Y-%m-%d") for col in quarterly_financials.columns[:10]
            ]
            print(f"분기별 데이터 예시 (최근 10개): {quarters}")

            # 분기별 데이터의 전체 기간 확인
            if len(quarterly_financials.columns) > 1:
                first_date = quarterly_financials.columns[-1]
                last_date = quarterly_financials.columns[0]
                print(
                    f"분기별 데이터 전체 기간: {first_date.strftime('%Y-%m-%d')} ~ {last_date.strftime('%Y-%m-%d')}"
                )
        print()

        # 1. 기본 정보 출력
        print("[1] 기본 정보")
        print("-" * 60)
        info = stock.info
        print(f"회사명: {info.get('longName', 'N/A')}")
        print(f"섹터: {info.get('sector', 'N/A')}")
        print(f"산업: {info.get('industry', 'N/A')}")
        print(f"현재가: ${info.get('currentPrice', 'N/A')}")
        print(f"시가총액: ${info.get('marketCap', 'N/A'):,}")

        # 2. 손익계산서 (Income Statement)
        print(f"\n[2] 손익계산서 (최근10년)")
        print("-" * 60)
        financials = stock.financials
        if not financials.empty:
            print(f"데이터 연도: {[col.year for col in financials.columns[:10]]}")
            print(f"\n주요 항목:")

            # Total Revenue
            if "Total Revenue" in financials.index:
                revenues = financials.loc["Total Revenue"][:10]
                print(f"  매출액 (Total Revenue):")
                for date, value in revenues.items():
                    print(f"    {date.year}: ${value:,.0f}")

            # Net Income
            if "Net Income" in financials.index:
                net_incomes = financials.loc["Net Income"][:10]
                print(f"\n  순이익 (Net Income):")
                for date, value in net_incomes.items():
                    print(f"    {date.year}: ${value:,.0f}")
        else:
            print("손익계산서 데이터 없음")

        # 3. 재무상태표 (Balance Sheet)
        print(f"\n[3] 재무상태표 (최근10년)")
        print("-" * 60)
        balance_sheet = stock.balance_sheet
        if not balance_sheet.empty:
            print(f"데이터 연도: {[col.year for col in balance_sheet.columns[:10]]}")
            print(f"\n주요 항목:")

            # Total Assets
            if "Total Assets" in balance_sheet.index:
                assets = balance_sheet.loc["Total Assets"][:10]
                print(f"  총 자산 (Total Assets):")
                for date, value in assets.items():
                    print(f"    {date.year}: ${value:,.0f}")

            # Total Equity
            if "Stockholders Equity" in balance_sheet.index:
                equities = balance_sheet.loc["Stockholders Equity"][:10]
                print(f"\n  총 자본 (Total Equity):")
                for date, value in equities.items():
                    print(f"    {date.year}: ${value:,.0f}")

            # Total Liabilities
            if "Total Liabilities Net Minority Interest" in balance_sheet.index:
                liabilities = balance_sheet.loc[
                    "Total Liabilities Net Minority Interest"
                ][:10]
                print(f"\n  총 부채 (Total Liabilities):")
                for date, value in liabilities.items():
                    print(f"    {date.year}: ${value:,.0f}")
        else:
            print("재무상태표 데이터 없음")

        # 4. 현금흐름표 (Cash Flow)
        print(f"\n[4] 현금흐름표 (최근10년)")
        print("-" * 60)
        cashflow = stock.cashflow
        if not cashflow.empty:
            print(f"데이터 연도: {[col.year for col in cashflow.columns[:10]]}")
            print(f"\n주요 항목:")

            # Operating Cash Flow
            if "Operating Cash Flow" in cashflow.index:
                ocf = cashflow.loc["Operating Cash Flow"][:10]
                print(f"  영업현금흐름 (Operating Cash Flow):")
                for date, value in ocf.items():
                    print(f"    {date.year}: ${value:,.0f}")

            # Free Cash Flow
            if "Free Cash Flow" in cashflow.index:
                fcf = cashflow.loc["Free Cash Flow"][:10]
                print(f"\n  잉여현금흐름 (Free Cash Flow):")
                for date, value in fcf.items():
                    print(f"    {date.year}: ${value:,.0f}")
        else:
            print("현금흐름표 데이터 없음")

        # 5. 사용 가능한 모든 재무 데이터 항목 출력
        print(f"\n[5] 손익계산서 사용 가능한 항목 목록")
        print("-" * 60)
        if not financials.empty:
            for idx, item in enumerate(financials.index, 1):
                print(f"{idx:2d}. {item}")

        print(f"\n[성공] 데이터 수집 성공!")
        return True

    except Exception as e:
        print(f"\n[오류] 오류 발생: {str(e)}")
        import traceback

        traceback.print_exc()
        return False


def main():
    """
    메인 실행 함수
    """
    print("\n" + "=" * 60)
    print("yfinance 데이터 수집 테스트")
    print("=" * 60)

    # 테스트할 종목 리스트
    test_tickers = ["AAPL", "MSFT", "GOOGL"]

    print(f"\n테스트 대상 종목: {', '.join(test_tickers)}")
    print(f"테스트 시작 시간: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    # 각 종목별 테스트
    results = {}
    for ticker in test_tickers:
        success = test_stock_data(ticker)
        results[ticker] = success

    # 결과 요약
    print(f"\n{'=' * 60}")
    print("테스트 결과 요약")
    print(f"{'=' * 60}")
    for ticker, success in results.items():
        status = "[성공]" if success else "[실패]"
        print(f"{ticker}: {status}")

    print(f"\n테스트 종료 시간: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60 + "\n")


if __name__ == "__main__":
    main()
