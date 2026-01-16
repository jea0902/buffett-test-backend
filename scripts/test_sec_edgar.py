"""
edgartools의 Financials 기능을 사용한 더 간단한 접근
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


def extract_using_financials(ticker, years=10):
    """
    edgartools의 Financials 클래스를 직접 사용
    """
    print(f"\n{'=' * 80}")
    print(f"SEC EDGAR 데이터 수집: {ticker}")
    print(f"{'=' * 80}\n")

    try:
        company = Company(ticker)

        print(f"[1단계] {ticker} 회사 정보")
        print(f"   ✓ 회사명: {company.name}")
        print(f"   ✓ CIK: {company.cik}")

        print(f"\n[2단계] 재무제표 가져오기...")

        # edgartools의 간단한 방법
        filings_obj = company.get_filings(form="10-K").latest(years)
        filings = list(filings_obj)

        all_data = []

        for idx, filing in enumerate(filings, 1):
            print(f"\n   [{idx}/{len(filings)}] {filing.filing_date} 처리 중...")

            try:
                # XBRL 가져오기
                xbrl = filing.xbrl()

                if not xbrl:
                    print("      ✗ XBRL 없음")
                    continue

                # statements 객체 사용
                statements = xbrl.statements

                print(f"      사용 가능한 재무제표:")
                for stmt in statements:
                    print(f"         - {stmt}")

                # 각 재무제표를 DataFrame으로 변환
                print(f"\n      재무제표 DataFrame 변환 중...")

                # 손익계산서 찾기
                income_df = None
                for stmt_name in statements:
                    if (
                        "OPERATIONS" in stmt_name.upper()
                        or "INCOME" in stmt_name.upper()
                    ):
                        if "COMPREHENSIVE" not in stmt_name.upper():  # 포괄손익은 제외
                            try:
                                stmt_obj = getattr(statements, stmt_name)
                                if hasattr(stmt_obj, "to_dataframe"):
                                    income_df = stmt_obj.to_dataframe()
                                    print(f"         ✓ 손익계산서: {stmt_name}")
                                    break
                            except:
                                pass

                # 재무상태표 찾기
                balance_df = None
                for stmt_name in statements:
                    if "BALANCE" in stmt_name.upper():
                        try:
                            stmt_obj = getattr(statements, stmt_name)
                            if hasattr(stmt_obj, "to_dataframe"):
                                balance_df = stmt_obj.to_dataframe()
                                print(f"         ✓ 재무상태표: {stmt_name}")
                                break
                        except:
                            pass

                # 현금흐름표 찾기
                cash_df = None
                for stmt_name in statements:
                    if "CASH" in stmt_name.upper():
                        try:
                            stmt_obj = getattr(statements, stmt_name)
                            if hasattr(stmt_obj, "to_dataframe"):
                                cash_df = stmt_obj.to_dataframe()
                                print(f"         ✓ 현금흐름표: {stmt_name}")
                                break
                        except:
                            pass

                # DataFrame 출력해서 구조 확인
                if income_df is not None:
                    print(f"\n      손익계산서 샘플:")
                    print(income_df.head(10))

                if balance_df is not None:
                    print(f"\n      재무상태표 샘플:")
                    print(balance_df.head(10))

                if cash_df is not None:
                    print(f"\n      현금흐름표 샘플:")
                    print(cash_df.head(10))

            except Exception as e:
                print(f"      ✗ 오류: {str(e)}")
                import traceback

                traceback.print_exc()
                continue

        return None

    except Exception as e:
        print(f"\n✗ 오류 발생: {str(e)}")
        import traceback

        traceback.print_exc()
        return None


if __name__ == "__main__":
    extract_using_financials("AAPL", years=3)  # 3년만 테스트
