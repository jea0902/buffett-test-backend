# 📊 데이터 수집 스크립트

yfinance를 사용하여 미국 주식의 재무 데이터를 수집하고 Oracle DB에 저장하는 Python 스크립트입니다.

## 🚀 시작하기

### 1. Python 가상환경 생성 및 활성화

```bash
# 프로젝트 루트 디렉토리에서 실행
cd c:\Projects\Bitcos-backend

# 가상환경 생성
python -m venv venv

# 가상환경 활성화 (Windows)
venv\Scripts\activate

# 가상환경 활성화 확인 (프롬프트 앞에 (venv) 표시됨)
```

### 2. 필요한 라이브러리 설치

```bash
# scripts 디렉토리로 이동
cd scripts

# 라이브러리 설치
pip install -r requirements.txt
```

### 3. 환경 변수 설정

```bash
# .env.example을 .env로 복사
copy .env.example .env

# .env 파일을 열어서 필요시 DB 접속 정보 수정
# (현재는 기본값으로 설정되어 있음)
```

### 4. yfinance 테스트 실행

```bash
# 데이터 수집이 정상적으로 되는지 테스트
python test_yfinance.py
```

**예상 출력:**
- AAPL, MSFT, GOOGL 종목의 재무 데이터가 출력됨
- 손익계산서, 재무상태표, 현금흐름표 데이터 확인 가능

## 📁 파일 구조

```
scripts/
├── requirements.txt          # Python 라이브러리 의존성
├── .env.example             # 환경 변수 템플릿
├── .env                     # 실제 환경 변수 (Git에 커밋 안 됨)
├── test_yfinance.py         # yfinance 데이터 수집 테스트
├── collect_financial_data.py # 실제 데이터 수집 및 DB 저장 (작성 예정)
└── README.md                # 이 파일
```

## ⚠️ 주의사항

### Oracle Instant Client 설치 필요

Python에서 Oracle DB에 연결하려면 **Oracle Instant Client**가 필요합니다.

**설치 방법:**

1. **다운로드**
   - https://www.oracle.com/database/technologies/instant-client/downloads.html
   - Windows용 Basic Package 다운로드

2. **설치**
   - ZIP 파일 압축 해제 (예: `C:\oracle\instantclient_19_8`)
   - 시스템 환경 변수 PATH에 경로 추가

3. **확인**
   ```bash
   # Python에서 테스트
   python -c "import cx_Oracle; print(cx_Oracle.version)"
   ```

### Docker Oracle DB 연결 확인

Docker에서 Oracle DB가 실행 중인지 확인:

```bash
# Docker 컨테이너 확인
docker ps

# Oracle DB 컨테이너가 실행 중이어야 함
# 포트 1521이 열려 있는지 확인
```

## 🔧 문제 해결

### cx_Oracle 설치 오류

**오류:** `cx_Oracle` 설치 중 오류 발생

**해결:**
```bash
# Oracle Instant Client 경로 확인
# 환경 변수 PATH에 추가되어 있는지 확인

# 또는 Python 코드에서 직접 경로 지정
import cx_Oracle
cx_Oracle.init_oracle_client(lib_dir=r"C:\oracle\instantclient_19_8")
```

### yfinance 데이터 수집 실패

**오류:** 특정 종목의 데이터가 수집되지 않음

**원인:**
- 인터넷 연결 문제
- 잘못된 티커 심볼
- Yahoo Finance 서버 일시적 문제

**해결:**
- 인터넷 연결 확인
- 티커 심볼 확인 (대문자로 입력)
- 잠시 후 다시 시도

## 📝 다음 단계

1. ✅ yfinance 테스트 완료
2. ⏳ Oracle DB 연결 테스트
3. ⏳ 데이터 수집 및 저장 스크립트 작성
4. ⏳ 배치 실행 스크립트 작성
5. ⏳ 스케줄러 설정 (자동화)
