package com.buffettTest.demo.controller;

import com.buffettTest.demo.entity.Board;
import com.buffettTest.demo.entity.BoardImage;
import com.buffettTest.demo.service.BoardImageService;
import com.buffettTest.demo.service.BoardLikeService;
import com.buffettTest.demo.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Board API", description = "게시판, 좋아요, 이미지 관련 API")
@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final BoardLikeService boardLikeService;
    private final BoardImageService boardImageService;

    @Operation(summary = "게시글 작성", description = "새로운 게시글을 등록합니다.")
    @PostMapping
    public ResponseEntity<Long> createPost(@RequestBody Map<String, String> request) {
        Long userId = Long.parseLong(request.get("userId"));
        String title = request.get("title");
        String content = request.get("content");

        Long postId = boardService.createPost(userId, title, content);
        return ResponseEntity.ok(postId);
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글 내용과 함께 이미지 목록, 좋아요 수를 조회합니다.")
    @GetMapping("/{postId}")
    public ResponseEntity<?> getPostDetail(@PathVariable Long postId) {
        // 1. 게시글 본문 조회 (조회수 증가 포함)
        Board board = boardService.getPost(postId);
        // 2. 관련 이미지 조회
        List<BoardImage> images = boardImageService.getImagesByPost(postId);
        // 3. 좋아요 수 조회
        long likeCount = boardLikeService.getLikeCount(postId);

        // 실제로는 DTO를 만들어야 하지만, MVP 속도를 위해 Map으로 묶어서 반환합니다.
        return ResponseEntity.ok(Map.of(
                "post", board,
                "images", images,
                "likeCount", likeCount));
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 Soft Delete 합니다.")
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId) {
        boardService.deletePost(postId);
        return ResponseEntity.ok("삭제되었습니다.");
    }

    @Operation(summary = "좋아요 토글", description = "좋아요를 추가하거나 취소합니다.")
    @PostMapping("/{postId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long postId, @RequestParam Long userId) {
        boardLikeService.toggleLike(userId, postId);
        return ResponseEntity.ok("좋아요 상태가 변경되었습니다.");
    }

    @Operation(summary = "이미지 추가", description = "게시글에 이미지 경로를 추가합니다.")
    @PostMapping("/{postId}/images")
    public ResponseEntity<?> addImage(@PathVariable Long postId, @RequestBody Map<String, Object> request) {
        String imageUrl = (String) request.get("imageUrl");
        Integer sortOrder = (Integer) request.get("sortOrder");
        boardImageService.addImage(postId, imageUrl, sortOrder);
        return ResponseEntity.ok("이미지가 추가되었습니다.");
    }
}