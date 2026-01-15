"""
yfinance 데이터 수집 테스트 스크립트

목적: yfinance 라이브러리가 정상적으로 데이터를 가져오는지 테스트
사용법: python test_yfinance.py
"""

import yfinance as yf
import pandas as pd
from datetime import datetime

def test_stock_data(ticker):
    """
    특정 종목의 데이터를 수집하여 출력
    
    Args:
        ticker (str): 종목 티커 (예: 'AAPL', 'MSFT')
    """
    print(f"\n{'='*60}")
    print(f"종목 데이터 수집 테스트: {ticker}")
    print(f"{'='*60}\n")
    
    try:
        # yfinance Ticker 객체 생성
        stock = yf.Ticker(ticker)
        
        # 1. 기본 정보 출력
        print("📊 [1] 기본 정보")
        print("-" * 60)
        info = stock.info
        print(f"회사명: {info.get('longName', 'N/A')}")
        print(f"섹터: {info.get('sector', 'N/A')}")
        print(f"산업: {info.get('industry', 'N/A')}")
        print(f"현재가: ${info.get('currentPrice', 'N/A')}")
        print(f"시가총액: ${info.get('marketCap', 'N/A'):,}")
        
        # 2. 손익계산서 (Income Statement)
        print(f"\n💰 [2] 손익계산서 (최근 5년)")
        print("-" * 60)
        financials = stock.financials
        if not financials.empty:
            print(f"데이터 연도: {[col.year for col in financials.columns[:5]]}")
            print(f"\n주요 항목:")
            
            # Total Revenue
            if 'Total Revenue' in financials.index:
                revenues = financials.loc['Total Revenue'][:5]
                print(f"  매출액 (Total Revenue):")
                for date, value in revenues.items():
                    print(f"    {date.year}: ${value:,.0f}")
            
            # Net Income
            if 'Net Income' in financials.index:
                net_incomes = financials.loc['Net Income'][:5]
                print(f"\n  순이익 (Net Income):")
                for date, value in net_incomes.items():
                    print(f"    {date.year}: ${value:,.0f}")
        else:
            print("손익계산서 데이터 없음")
        
        # 3. 재무상태표 (Balance Sheet)
        print(f"\n🏦 [3] 재무상태표 (최근 5년)")
        print("-" * 60)
        balance_sheet = stock.balance_sheet
        if not balance_sheet.empty:
            print(f"데이터 연도: {[col.year for col in balance_sheet.columns[:5]]}")
            print(f"\n주요 항목:")
            
            # Total Assets
            if 'Total Assets' in balance_sheet.index:
                assets = balance_sheet.loc['Total Assets'][:5]
                print(f"  총 자산 (Total Assets):")
                for date, value in assets.items():
                    print(f"    {date.year}: ${value:,.0f}")
            
            # Total Equity
            if 'Stockholders Equity' in balance_sheet.index:
                equities = balance_sheet.loc['Stockholders Equity'][:5]
                print(f"\n  총 자본 (Total Equity):")
                for date, value in equities.items():
                    print(f"    {date.year}: ${value:,.0f}")
            
            # Total Liabilities
            if 'Total Liabilities Net Minority Interest' in balance_sheet.index:
                liabilities = balance_sheet.loc['Total Liabilities Net Minority Interest'][:5]
                print(f"\n  총 부채 (Total Liabilities):")
                for date, value in liabilities.items():
                    print(f"    {date.year}: ${value:,.0f}")
        else:
            print("재무상태표 데이터 없음")
        
        # 4. 현금흐름표 (Cash Flow)
        print(f"\n💵 [4] 현금흐름표 (최근 5년)")
        print("-" * 60)
        cashflow = stock.cashflow
        if not cashflow.empty:
            print(f"데이터 연도: {[col.year for col in cashflow.columns[:5]]}")
            print(f"\n주요 항목:")
            
            # Operating Cash Flow
            if 'Operating Cash Flow' in cashflow.index:
                ocf = cashflow.loc['Operating Cash Flow'][:5]
                print(f"  영업현금흐름 (Operating Cash Flow):")
                for date, value in ocf.items():
                    print(f"    {date.year}: ${value:,.0f}")
            
            # Free Cash Flow
            if 'Free Cash Flow' in cashflow.index:
                fcf = cashflow.loc['Free Cash Flow'][:5]
                print(f"\n  잉여현금흐름 (Free Cash Flow):")
                for date, value in fcf.items():
                    print(f"    {date.year}: ${value:,.0f}")
        else:
            print("현금흐름표 데이터 없음")
        
        # 5. 사용 가능한 모든 재무 데이터 항목 출력
        print(f"\n📋 [5] 손익계산서 사용 가능한 항목 목록")
        print("-" * 60)
        if not financials.empty:
            for idx, item in enumerate(financials.index, 1):
                print(f"{idx:2d}. {item}")
        
        print(f"\n✅ 데이터 수집 성공!")
        return True
        
    except Exception as e:
        print(f"\n❌ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        return False


def main():
    """
    메인 실행 함수
    """
    print("\n" + "="*60)
    print("yfinance 데이터 수집 테스트")
    print("="*60)
    
    # 테스트할 종목 리스트
    test_tickers = ['AAPL', 'MSFT', 'GOOGL']
    
    print(f"\n테스트 대상 종목: {', '.join(test_tickers)}")
    print(f"테스트 시작 시간: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    # 각 종목별 테스트
    results = {}
    for ticker in test_tickers:
        success = test_stock_data(ticker)
        results[ticker] = success
    
    # 결과 요약
    print(f"\n{'='*60}")
    print("테스트 결과 요약")
    print(f"{'='*60}")
    for ticker, success in results.items():
        status = "✅ 성공" if success else "❌ 실패"
        print(f"{ticker}: {status}")
    
    print(f"\n테스트 종료 시간: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*60 + "\n")


if __name__ == "__main__":
    main()
