package com.buffettTest.demo.repository;

import com.buffettTest.demo.entity.FinancialAnnual;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FinancialAnnualRepository extends JpaRepository<FinancialAnnual, Long> {

    // 특정 주식(stockId)의 모든 재무 데이터를 연도순으로 정렬해서 찾기
    // findBy 뒤에 'Stock_StockId'라고 적으면 JPA가 객체 내부의 ID를 알아서 찾아줍니다.
    List<FinancialAnnual> findByStock_StockIdOrderByFiscalYearDesc(Long stockId);
}