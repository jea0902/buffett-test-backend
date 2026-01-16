"""
SEC EDGAR 데이터를 pandas DataFrame으로 변환하는 스크립트 (수정 완료)
"""

import sys
import io

if sys.platform == "win32":
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

from edgar import Company, set_identity
from datetime import datetime
import pandas as pd
import warnings

warnings.filterwarnings("ignore")

set_identity("Bitcos Test bitcos@example.com")


def extract_statement_value(statement_data, concept_name, period_key=None):
    """
    재무제표 데이터에서 특정 concept의 값을 추출

    Args:
        statement_data: get_statement()로 받은 리스트 데이터
        concept_name: 찾을 concept 이름 (예: 'us-gaap_NetIncomeLoss')
        period_key: 특정 기간 키 (None이면 첫 번째 값 반환)

    Returns:
        값 또는 None
    """
    for item in statement_data:
        if item.get("concept") == concept_name or item.get("name") == concept_name:
            values = item.get("values", {})
            if not values:
                continue

            if period_key and period_key in values:
                return values[period_key]
            elif values:
                # 첫 번째 값 반환 (가장 최근)
                return list(values.values())[0]

    return None


def get_period_keys_from_statement(statement_data):
    """재무제표에서 사용 가능한 기간 키들을 추출"""
    for item in statement_data:
        values = item.get("values", {})
        if values:
            return list(values.keys())
    return []


def extract_financial_data_to_dataframe(ticker, years=10):
    """
    특정 종목의 재무데이터를 DataFrame으로 변환
    """
    print(f"\n{'=' * 80}")
    print(f"SEC EDGAR 데이터 수집: {ticker}")
    print(f"{'=' * 80}\n")

    try:
        # [1] Company 객체 생성
        print(f"[1단계] {ticker} 회사 정보 로딩 중...")
        company = Company(ticker)

        company_info = {"회사명": company.name, "CIK": company.cik, "Ticker": ticker}

        print(f"   ✓ 회사명: {company.name}")
        print(f"   ✓ CIK: {company.cik}")

        # [2] 10-K 파일링 가져오기
        print(f"\n[2단계] 최근 {years}년치 10-K 파일링 가져오는 중...")
        filings_obj = company.get_filings(form="10-K").latest(years)

        # EntityFilings 객체를 리스트로 변환
        filings = list(filings_obj)

        print(f"   ✓ 찾은 10-K 파일링 개수: {len(filings)}개")

        if len(filings) == 0:
            print(f"   ✗ {ticker}에 대한 10-K 파일링을 찾을 수 없습니다.")
            return None

        # [3] 각 연도별 재무데이터 수집
        print(f"\n[3단계] 재무데이터 추출 중...")

        all_data = []

        for idx, filing in enumerate(filings, 1):
            filing_date = filing.filing_date
            fiscal_year = filing_date.year

            print(
                f"   [{idx}/{len(filings)}] {fiscal_year}년 데이터 처리 중...", end=" "
            )

            try:
                xbrl = filing.xbrl()

                if xbrl is None:
                    print("✗ XBRL 데이터 없음")
                    continue

                # 재무제표 가져오기 - 여러 이름으로 시도
                income_stmt = None
                balance_sheet = None
                cash_flow = None

                # 손익계산서는 여러 이름으로 존재할 수 있음
                try:
                    income_stmt = xbrl.get_statement(
                        "operations"
                    ) or xbrl.get_statement("income")
                except:
                    try:
                        income_stmt = xbrl.get_statement("income")
                    except:
                        pass

                try:
                    balance_sheet = xbrl.get_statement("balance")
                except:
                    pass

                try:
                    cash_flow = xbrl.get_statement("cash")
                except:
                    pass

                if not income_stmt or not balance_sheet:
                    print("✗ 재무제표 없음")
                    continue

                # 사용 가능한 기간 키 가져오기
                period_keys = get_period_keys_from_statement(income_stmt)
                if not period_keys:
                    print("✗ 기간 정보 없음")
                    continue

                # 가장 최근 기간 (첫 번째) 사용
                current_period = period_keys[0]

                # 데이터 추출
                year_data = {
                    "Fiscal Year": fiscal_year,
                    "Filing Date": filing_date.strftime("%Y-%m-%d"),
                    "Period": current_period,
                }

                # 손익계산서 항목 - 여러 가능한 concept 이름으로 시도
                year_data["Revenue"] = (
                    extract_statement_value(
                        income_stmt, "us-gaap_Revenues", current_period
                    )
                    or extract_statement_value(
                        income_stmt, "us-gaap_SalesRevenueNet", current_period
                    )
                    or extract_statement_value(
                        income_stmt,
                        "us-gaap_RevenueFromContractWithCustomerExcludingAssessedTax",
                        current_period,
                    )
                    or extract_statement_value(
                        income_stmt,
                        "us-gaap_RevenueFromContractWithCustomerIncludingAssessedTax",
                        current_period,
                    )
                )

                year_data["Net Income"] = extract_statement_value(
                    income_stmt, "us-gaap_NetIncomeLoss", current_period
                ) or extract_statement_value(
                    income_stmt, "us-gaap_ProfitLoss", current_period
                )

                # Operating Income을 EBIT로 사용 (일반적으로 유사함)
                year_data["EBIT"] = extract_statement_value(
                    income_stmt, "us-gaap_OperatingIncomeLoss", current_period
                ) or extract_statement_value(
                    income_stmt,
                    "us-gaap_IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest",
                    current_period,
                )

                year_data["Interest Expense"] = (
                    extract_statement_value(
                        income_stmt, "us-gaap_InterestExpense", current_period
                    )
                    or extract_statement_value(
                        income_stmt, "us-gaap_InterestExpenseDebt", current_period
                    )
                    or extract_statement_value(
                        income_stmt, "us-gaap_InterestAndDebtExpense", current_period
                    )
                )

                # 재무상태표 항목
                year_data["Total Assets"] = extract_statement_value(
                    balance_sheet, "us-gaap_Assets", current_period
                )

                year_data["Total Liabilities"] = extract_statement_value(
                    balance_sheet, "us-gaap_Liabilities", current_period
                )

                year_data["Total Equity"] = extract_statement_value(
                    balance_sheet, "us-gaap_StockholdersEquity", current_period
                ) or extract_statement_value(
                    balance_sheet,
                    "us-gaap_StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest",
                    current_period,
                )

                # 현금흐름표 항목
                if cash_flow:
                    year_data["Operating Cash Flow"] = extract_statement_value(
                        cash_flow,
                        "us-gaap_NetCashProvidedByUsedInOperatingActivities",
                        current_period,
                    ) or extract_statement_value(
                        cash_flow,
                        "us-gaap_CashProvidedByUsedInOperatingActivities",
                        current_period,
                    )

                    year_data["Capital Expenditure"] = extract_statement_value(
                        cash_flow,
                        "us-gaap_PaymentsToAcquirePropertyPlantAndEquipment",
                        current_period,
                    ) or extract_statement_value(
                        cash_flow, "us-gaap_CapitalExpenditures", current_period
                    )
                else:
                    year_data["Operating Cash Flow"] = None
                    year_data["Capital Expenditure"] = None

                # Free Cash Flow 계산
                ocf = year_data["Operating Cash Flow"]
                capex = year_data["Capital Expenditure"]
                if ocf and capex:
                    year_data["Free Cash Flow"] = ocf - abs(capex)
                else:
                    year_data["Free Cash Flow"] = None

                # 비율 계산
                revenue = year_data["Revenue"]
                net_income = year_data["Net Income"]
                total_equity = year_data["Total Equity"]
                total_assets = year_data["Total Assets"]
                ebit = year_data["EBIT"]

                # ROE
                if net_income and total_equity and total_equity != 0:
                    year_data["ROE (%)"] = round((net_income / total_equity) * 100, 2)
                else:
                    year_data["ROE (%)"] = None

                # Net Margin
                if net_income and revenue and revenue != 0:
                    year_data["Net Margin (%)"] = round((net_income / revenue) * 100, 2)
                else:
                    year_data["Net Margin (%)"] = None

                # ROIC (간이 계산)
                if ebit and total_assets and total_assets != 0:
                    year_data["ROIC (%)"] = round((ebit / total_assets) * 100, 2)
                else:
                    year_data["ROIC (%)"] = None

                # FCF Margin
                fcf = year_data["Free Cash Flow"]
                if fcf and revenue and revenue != 0:
                    year_data["FCF Margin (%)"] = round((fcf / revenue) * 100, 2)
                else:
                    year_data["FCF Margin (%)"] = None

                # Debt Ratio
                total_liabilities = year_data["Total Liabilities"]
                if total_liabilities and total_equity and total_equity != 0:
                    year_data["Debt Ratio (%)"] = round(
                        (total_liabilities / total_equity) * 100, 2
                    )
                else:
                    year_data["Debt Ratio (%)"] = None

                # Interest Coverage
                interest_expense = year_data["Interest Expense"]
                if ebit and interest_expense and interest_expense != 0:
                    year_data["Interest Coverage"] = round(ebit / interest_expense, 2)
                else:
                    year_data["Interest Coverage"] = None

                all_data.append(year_data)
                print("✓")

            except Exception as e:
                print(f"✗ 오류: {str(e)}")
                continue

        if not all_data:
            print("\n   ✗ 추출된 데이터가 없습니다.")
            return None

        # [4] DataFrame 생성
        print(f"\n[4단계] DataFrame 생성 중...")
        df = pd.DataFrame(all_data)

        # 연도 기준 내림차순 정렬
        df = df.sort_values("Fiscal Year", ascending=False).reset_index(drop=True)

        print(f"   ✓ {len(df)}개 연도 데이터 생성 완료")

        # [5] 결과 반환
        result = {
            "company_info": company_info,
            "full_data": df,
            "key_metrics": df[
                [
                    "Fiscal Year",
                    "ROE (%)",
                    "ROIC (%)",
                    "Net Margin (%)",
                    "FCF Margin (%)",
                    "Debt Ratio (%)",
                    "Interest Coverage",
                ]
            ],
            "income_statement": df[
                ["Fiscal Year", "Revenue", "Net Income", "EBIT", "Interest Expense"]
            ],
            "balance_sheet": df[
                ["Fiscal Year", "Total Assets", "Total Liabilities", "Total Equity"]
            ],
            "cash_flow": df[
                [
                    "Fiscal Year",
                    "Operating Cash Flow",
                    "Capital Expenditure",
                    "Free Cash Flow",
                ]
            ],
        }

        print(f"\n{'=' * 80}")
        print(f"데이터 수집 완료!")
        print(f"{'=' * 80}\n")

        return result

    except Exception as e:
        print(f"\n✗ 오류 발생: {str(e)}")
        import traceback

        traceback.print_exc()
        return None


def display_results(result):
    """결과를 보기 좋게 출력"""
    if result is None:
        print("표시할 데이터가 없습니다.")
        return

    # 회사 정보
    print("\n" + "=" * 80)
    print("📊 회사 기본 정보")
    print("=" * 80)
    for key, value in result["company_info"].items():
        print(f"{key}: {value}")

    # 주요 지표
    print("\n" + "=" * 80)
    print("📈 주요 평가 지표 (Warren Buffett Criteria)")
    print("=" * 80)
    print("\n[버핏의 우량주 평가 기준]")
    print("- ROE: 10년 중 8년 이상 15% 이상 유지")
    print("- ROIC: 10년 중 8년 이상 12% 이상 유지")
    print("- Net Margin: 높고 안정적일수록 좋음")
    print("- FCF Margin: 15% 이상이 우수")
    print("- Debt Ratio: 50% 이하가 우수")
    print("- Interest Coverage: 10배 이상이 우수\n")

    pd.set_option("display.max_columns", None)
    pd.set_option("display.width", None)
    pd.set_option("display.float_format", "{:.2f}".format)

    print(result["key_metrics"].to_string(index=False))

    # 손익계산서
    print("\n" + "=" * 80)
    print("💰 손익계산서 (단위: 백만)")
    print("=" * 80)
    income_df = result["income_statement"].copy()
    for col in ["Revenue", "Net Income", "EBIT", "Interest Expense"]:
        if col in income_df.columns:
            income_df[col + " (M)"] = income_df[col].fillna(0) / 1_000_000
            income_df[col + " (M)"] = income_df[col + " (M)"].round(2)
            income_df = income_df.drop(columns=[col])
    print(income_df.to_string(index=False))

    # 재무상태표
    print("\n" + "=" * 80)
    print("🏦 재무상태표 (단위: 백만)")
    print("=" * 80)
    balance_df = result["balance_sheet"].copy()
    for col in ["Total Assets", "Total Liabilities", "Total Equity"]:
        if col in balance_df.columns:
            balance_df[col + " (M)"] = balance_df[col].fillna(0) / 1_000_000
            balance_df[col + " (M)"] = balance_df[col + " (M)"].round(2)
            balance_df = balance_df.drop(columns=[col])
    print(balance_df.to_string(index=False))

    # 현금흐름표
    print("\n" + "=" * 80)
    print("💵 현금흐름표 (단위: 백만)")
    print("=" * 80)
    cash_df = result["cash_flow"].copy()
    for col in ["Operating Cash Flow", "Capital Expenditure", "Free Cash Flow"]:
        if col in cash_df.columns:
            cash_df[col + " (M)"] = cash_df[col].fillna(0) / 1_000_000
            cash_df[col + " (M)"] = cash_df[col + " (M)"].round(2)
            cash_df = cash_df.drop(columns=[col])
    print(cash_df.to_string(index=False))

    # 통계 요약
    print("\n" + "=" * 80)
    print("📊 통계 요약")
    print("=" * 80)

    metrics = result["key_metrics"]
    summary = {
        "ROE 평균 (%)": metrics["ROE (%)"].mean(),
        "ROE 표준편차": metrics["ROE (%)"].std(),
        "ROIC 평균 (%)": metrics["ROIC (%)"].mean(),
        "Net Margin 평균 (%)": metrics["Net Margin (%)"].mean(),
        "Net Margin 표준편차": metrics["Net Margin (%)"].std(),
        "FCF Margin 평균 (%)": metrics["FCF Margin (%)"].mean(),
        "Debt Ratio 평균 (%)": metrics["Debt Ratio (%)"].mean(),
        "Interest Coverage 평균": metrics["Interest Coverage"].mean(),
    }

    for key, value in summary.items():
        if pd.notna(value):
            print(f"{key}: {value:.2f}")
        else:
            print(f"{key}: N/A")

    # 버핏 기준 평가
    print("\n" + "=" * 80)
    print("⭐ 워렌 버핏 기준 간이 평가")
    print("=" * 80)

    roe_15_count = (metrics["ROE (%)"] >= 15).sum()
    roic_12_count = (metrics["ROIC (%)"] >= 12).sum()

    print(f"✓ ROE 15% 이상 달성 연도: {roe_15_count}/{len(metrics)}년")
    print(f"✓ ROIC 12% 이상 달성 연도: {roic_12_count}/{len(metrics)}년")
    print(f"✓ Net Margin 안정성: 표준편차 {metrics['Net Margin (%)'].std():.2f}%")

    latest_debt = metrics["Debt Ratio (%)"].iloc[0]
    if pd.notna(latest_debt):
        print(f"✓ 최근 부채비율: {latest_debt:.2f}%")

    latest_coverage = metrics["Interest Coverage"].iloc[0]
    if pd.notna(latest_coverage):
        print(f"✓ 최근 이자보상배율: {latest_coverage:.2f}배")

    print("\n" + "=" * 80 + "\n")


def main():
    """메인 실행 함수"""
    print("\n" + "=" * 80)
    print("SEC EDGAR 재무데이터 → pandas DataFrame 변환")
    print("=" * 80)

    ticker = "AAPL"

    print(f"\n테스트 종목: {ticker}")
    print(f"시작 시간: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

    # 데이터 수집
    result = extract_financial_data_to_dataframe(ticker, years=10)

    # 결과 출력
    if result:
        display_results(result)

        # CSV 저장
        save_option = input("\nDataFrame을 CSV 파일로 저장하시겠습니까? (y/n): ")
        if save_option.lower() == "y":
            filename = (
                f"{ticker}_financial_data_{datetime.now().strftime('%Y%m%d')}.csv"
            )
            result["full_data"].to_csv(filename, index=False, encoding="utf-8-sig")
            print(f"✓ 저장 완료: {filename}")
    else:
        print("\n✗ 데이터 수집 실패")

    print(f"\n종료 시간: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 80 + "\n")


if __name__ == "__main__":
    main()
