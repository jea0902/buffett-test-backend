"""
재무제표 데이터 진단 도구

목적: yfinance에서 가져온 원본 데이터를 상세하게 분석
- 어떤 연도 데이터가 있는지
- 각 항목의 실제 값 확인
- 누락된 항목 찾기
"""

# 결과 : 2021년도 모든 주요 데이터가 NaN이라 사용불가
# 이자 비용은 NaN일 경우 0으로 처리해야 함

import yfinance as yf
from curl_cffi.requests import Session
import pandas as pd
from datetime import datetime

# SSL 인증서 에러 우회용 세션 생성
session = Session(impersonate="chrome")
session.verify = False


def format_number(value):
    """숫자를 읽기 쉽게 포맷"""
    if pd.isna(value):
        return "❌ NaN"
    elif value == 0:
        return "⚠️  0"
    elif abs(value) >= 1e12:
        return f"${value / 1e12:,.2f}T"
    elif abs(value) >= 1e9:
        return f"${value / 1e9:,.2f}B"
    elif abs(value) >= 1e6:
        return f"${value / 1e6:,.2f}M"
    elif abs(value) >= 1e3:
        return f"${value / 1e3:,.2f}K"
    else:
        return f"${value:,.2f}"


def check_field_availability(df, field_name):
    """특정 필드가 있는지 확인하고 값 반환"""
    if field_name in df.index:
        return "✅ 존재", df.loc[field_name]
    else:
        return "❌ 없음", None


def diagnose_stock(ticker):
    """
    종목의 재무 데이터를 상세히 진단

    Args:
        ticker (str): 종목 티커
    """
    print("\n" + "=" * 100)
    print(f"🔍 재무제표 데이터 진단: {ticker}")
    print("=" * 100)
    print(f"⏰ 진단 시작: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

    try:
        stock = yf.Ticker(ticker, session=session)

        # 기본 정보
        info = stock.info
        print("📋 기본 정보")
        print("-" * 100)
        print(f"회사명: {info.get('longName', 'N/A')}")
        print(f"섹터: {info.get('sector', 'N/A')}")
        print(f"산업: {info.get('industry', 'N/A')}")
        print(f"현재가: ${info.get('currentPrice', 0):.2f}")

        # 재무제표 가져오기
        financials = stock.financials  # 손익계산서
        balance_sheet = stock.balance_sheet  # 재무상태표
        cashflow = stock.cashflow  # 현금흐름표

        # ================================================================
        # 1. 손익계산서 (Income Statement) 진단
        # ================================================================
        print("\n\n" + "=" * 100)
        print("📊 손익계산서 (Income Statement)")
        print("=" * 100)

        if financials.empty:
            print("❌ 손익계산서 데이터가 없습니다!")
        else:
            # 연도 정보
            years = [col.year for col in financials.columns]
            print(f"\n📅 사용 가능한 연도: {years}")
            print(f"📊 총 {len(years)}년치 데이터\n")

            # 주요 항목들 체크
            key_fields = [
                "Total Revenue",
                "Cost Of Revenue",
                "Gross Profit",
                "Operating Revenue",
                "Operating Expense",
                "Operating Income",
                "EBITDA",
                "EBIT",
                "Interest Expense",
                "Interest Income",
                "Pretax Income",
                "Tax Provision",
                "Net Income",
                "Net Income Common Stockholders",
                "Diluted EPS",
                "Basic EPS",
            ]

            print(f"{'필드명':<45} {'상태':<10} {' | '.join([str(y) for y in years])}")
            print("-" * 100)

            for field in key_fields:
                status, values = check_field_availability(financials, field)

                if status == "✅ 존재":
                    value_str = " | ".join(
                        [format_number(values[col]) for col in financials.columns]
                    )
                    print(f"{field:<45} {status:<10} {value_str}")
                else:
                    print(f"{field:<45} {status:<10}")

            # 전체 필드 목록
            print("\n\n📋 손익계산서 전체 필드 목록:")
            print("-" * 100)
            for idx, field in enumerate(financials.index, 1):
                print(f"{idx:2d}. {field}")

        # ================================================================
        # 2. 재무상태표 (Balance Sheet) 진단
        # ================================================================
        print("\n\n" + "=" * 100)
        print("🏦 재무상태표 (Balance Sheet)")
        print("=" * 100)

        if balance_sheet.empty:
            print("❌ 재무상태표 데이터가 없습니다!")
        else:
            years = [col.year for col in balance_sheet.columns]
            print(f"\n📅 사용 가능한 연도: {years}")
            print(f"📊 총 {len(years)}년치 데이터\n")

            key_fields = [
                "Total Assets",
                "Current Assets",
                "Cash And Cash Equivalents",
                "Total Liabilities Net Minority Interest",
                "Current Liabilities",
                "Total Debt",
                "Long Term Debt",
                "Current Debt",
                "Stockholders Equity",
                "Common Stock",
                "Retained Earnings",
                "Working Capital",
            ]

            print(f"{'필드명':<45} {'상태':<10} {' | '.join([str(y) for y in years])}")
            print("-" * 100)

            for field in key_fields:
                status, values = check_field_availability(balance_sheet, field)

                if status == "✅ 존재":
                    value_str = " | ".join(
                        [format_number(values[col]) for col in balance_sheet.columns]
                    )
                    print(f"{field:<45} {status:<10} {value_str}")
                else:
                    print(f"{field:<45} {status:<10}")

            print("\n\n📋 재무상태표 전체 필드 목록:")
            print("-" * 100)
            for idx, field in enumerate(balance_sheet.index, 1):
                print(f"{idx:2d}. {field}")

        # ================================================================
        # 3. 현금흐름표 (Cash Flow Statement) 진단
        # ================================================================
        print("\n\n" + "=" * 100)
        print("💰 현금흐름표 (Cash Flow Statement)")
        print("=" * 100)

        if cashflow.empty:
            print("❌ 현금흐름표 데이터가 없습니다!")
        else:
            years = [col.year for col in cashflow.columns]
            print(f"\n📅 사용 가능한 연도: {years}")
            print(f"📊 총 {len(years)}년치 데이터\n")

            key_fields = [
                "Operating Cash Flow",
                "Investing Cash Flow",
                "Financing Cash Flow",
                "Free Cash Flow",
                "Capital Expenditure",
                "Issuance Of Debt",
                "Repayment Of Debt",
                "Repurchase Of Capital Stock",
                "Cash Dividends Paid",
            ]

            print(f"{'필드명':<45} {'상태':<10} {' | '.join([str(y) for y in years])}")
            print("-" * 100)

            for field in key_fields:
                status, values = check_field_availability(cashflow, field)

                if status == "✅ 존재":
                    value_str = " | ".join(
                        [format_number(values[col]) for col in cashflow.columns]
                    )
                    print(f"{field:<45} {status:<10} {value_str}")
                else:
                    print(f"{field:<45} {status:<10}")

            print("\n\n📋 현금흐름표 전체 필드 목록:")
            print("-" * 100)
            for idx, field in enumerate(cashflow.index, 1):
                print(f"{idx:2d}. {field}")

        # ================================================================
        # 4. 문제 진단 요약
        # ================================================================
        print("\n\n" + "=" * 100)
        print("⚠️ 문제 진단 요약")
        print("=" * 100)

        issues = []

        # Interest Expense 체크
        if not financials.empty:
            status, values = check_field_availability(financials, "Interest Expense")
            if status == "✅ 존재":
                has_zero = any(v == 0 or pd.isna(v) for v in values)
                if has_zero:
                    issues.append(
                        "🔸 Interest Expense가 0이거나 NaN인 연도가 있음 (무차입 경영 가능성)"
                    )
            else:
                issues.append("🔴 Interest Expense 필드가 없음")

        # EPS 체크
        if not financials.empty:
            diluted_status, diluted_values = check_field_availability(
                financials, "Diluted EPS"
            )
            basic_status, basic_values = check_field_availability(
                financials, "Basic EPS"
            )

            if diluted_status == "❌ 없음" and basic_status == "❌ 없음":
                issues.append("🔴 EPS 데이터가 없음 (Diluted EPS, Basic EPS 모두 없음)")
            elif diluted_status == "✅ 존재":
                has_nan = any(pd.isna(v) for v in diluted_values)
                if has_nan:
                    issues.append("🔸 Diluted EPS에 NaN 값이 있음")

        # 2021년 데이터 체크
        if not financials.empty:
            years = [col.year for col in financials.columns]
            if 2021 not in years:
                issues.append(
                    "🔸 2021년 데이터가 없음 (yfinance는 보통 최근 4년치만 제공)"
                )
            else:
                # 2021년 데이터 완전성 체크
                col_2021 = [col for col in financials.columns if col.year == 2021][0]
                revenue_2021 = (
                    financials.loc["Total Revenue", col_2021]
                    if "Total Revenue" in financials.index
                    else None
                )
                net_income_2021 = (
                    financials.loc["Net Income", col_2021]
                    if "Net Income" in financials.index
                    else None
                )

                if pd.isna(revenue_2021) or pd.isna(net_income_2021):
                    issues.append(
                        "🔴 2021년 데이터가 불완전함 (Revenue 또는 Net Income이 NaN)"
                    )

        if issues:
            print("\n발견된 문제:")
            for issue in issues:
                print(f"  {issue}")
        else:
            print("\n✅ 특별한 문제가 발견되지 않았습니다!")

        print("\n\n" + "=" * 100)
        print("✅ 진단 완료!")
        print("=" * 100)
        print(f"⏰ 진단 종료: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

    except Exception as e:
        print(f"\n❌ 오류 발생: {str(e)}")
        import traceback

        traceback.print_exc()


def main():
    """메인 실행 함수"""
    # AAPL 진단
    diagnose_stock("AAPL")

    print("\n" + "=" * 100)
    print("💡 다른 종목도 진단하려면 diagnose_stock('MSFT') 같은 형태로 호출하세요.")
    print("=" * 100 + "\n")


if __name__ == "__main__":
    main()
