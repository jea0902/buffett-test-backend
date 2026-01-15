package com.buffettTest.demo.controller;

import com.buffettTest.demo.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

// @Tag, @Operation 어노테이션들은 오직 Swagger(자동 API 문서화)를 위한 도구들
// 비즈니스 로직은 어차피 Service가 다 함

@Tag(name = "Trade API", description = "모의 투자 및 트레이딩뷰 웹훅 처리")
@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @Operation(summary = "포지션 오픈 (시장가/지정가)", description = "트레이딩뷰 신호 또는 사용자의 직접 주문을 처리합니다.")
    @PostMapping("/open")
    public ResponseEntity<?> openPosition(@RequestBody Map<String, Object> request) {
        // 하향식 설계: 요청 데이터를 받아서 서비스로 넘김
        Long userId = Long.parseLong(request.get("userId").toString());
        String ticker = (String) request.get("ticker");
        String side = (String) request.get("side");
        Integer leverage = Integer.parseInt(request.get("leverage").toString());
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        BigDecimal price = new BigDecimal(request.get("price").toString());
        String orderType = (String) request.get("orderType");

        Long tradeId = tradeService.openPosition(userId, ticker, side, leverage, amount, price, orderType);
        return ResponseEntity.ok("주문 성공. 거래 ID: " + tradeId);
    }

    @Operation(summary = "포지션 종료", description = "익절 또는 손절 시 포지션을 닫습니다.")
    @PostMapping("/close/{tradeId}")
    public ResponseEntity<?> closePosition(@PathVariable Long tradeId, @RequestParam BigDecimal exitPrice) {
        tradeService.closePosition(tradeId, exitPrice);
        return ResponseEntity.ok("포지션이 성공적으로 종료되었습니다.");
    }
}