package com.buffettTest.demo.controller;

import com.buffettTest.demo.entity.StockOfUsa;
import com.buffettTest.demo.service.StockOfUsaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 워렌 버핏 강력 추천 종목 리스트를 카드로 보여주기 위한 컨트롤러
// (ScoringService 필터에서 살아남은 정예 종목들만)
// 1. 버핏 기준 우량주는 뭐야?
// 2. 그 우량주들의 적정가는 얼마야?
// 3. 우량주면서 적정가 아래인 저평가 종목은 뭐야?
// 4. 우량주인 이유, 저평가 종목인 이유 요약 텍스트 추가
// 추가) 우량주마다 갖고 있는 산업 종류도 필터링 할 수 있어야 할 듯?

@Tag(name = "Buffett Pick API", description = "워렌 버핏의 우량주/저평가 기준에 따른 카드 리스트")
@RestController
@RequestMapping("/api/buffett-picks")
@RequiredArgsConstructor
public class BuffettPickController {

    private final StockOfUsaService stockOfUsaService;

    @Operation(summary = "메인 화면용 카드 전체 조회", description = "우량주(빨간 카드)와 저평가 우량주(황금 카드)를 모두 가져옵니다.")
    @GetMapping("/cards")
    public ResponseEntity<?> getMainCards() {
        // 1. ScoringService에 의해 IsQualified='Y'로 판정된 모든 우량주 가져오기 (빨간 카드 후보)
        List<StockOfUsa> allQualified = stockOfUsaService.getQualifiedStocks();

        // 2. 그 중에서도 PriceStatus='STRONG_BUY'인 종목 가져오기 (황금 카드 후보)
        List<StockOfUsa> strongBuys = stockOfUsaService.getStrongBuyStocks();

        // 1차 MVP 속도를 위해 두 리스트를 Map에 담아 프론트로 보냅니다.
        // 프론트엔드에서는 priceStatus 값을 보고 황금색/빨간색 테두리를 입히게 됩니다.
        return ResponseEntity.ok(Map.of(
                "totalCount", allQualified.size(),
                "allQualifiedStocks", allQualified, // 빨간색 카드 기반 데이터
                "strongBuyStocks", strongBuys // 황금색 카드 기반 데이터
        ));
    }

    @Operation(summary = "강력 추천(황금 카드)만 보기", description = "저평가된 STRONG_BUY 종목만 따로 필터링하여 보여줍니다.")
    @GetMapping("/cards/gold")
    public ResponseEntity<List<StockOfUsa>> getGoldCards() {
        return ResponseEntity.ok(stockOfUsaService.getStrongBuyStocks());
    }

    @Operation(summary = "종목 상세 이유 조회", description = "왜 우량주인지, 왜 저평가인지 요약 텍스트를 포함한 상세 정보를 조회합니다.")
    @GetMapping("/detail/{ticker}")
    public ResponseEntity<StockOfUsa> getPickDetail(@PathVariable String ticker) {
        // StockOfUsa 엔티티에 저장된 '이유 요약' 필드들을 함께 가져옵니다.
        return ResponseEntity.ok(stockOfUsaService.getStockInfo(ticker));
    }
}