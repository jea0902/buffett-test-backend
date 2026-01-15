package com.buffettTest.demo.service;

import com.buffettTest.demo.entity.Board;
import com.buffettTest.demo.entity.BoardLike;
import com.buffettTest.demo.entity.User;
import com.buffettTest.demo.repository.BoardLikeRepository;
import com.buffettTest.demo.repository.BoardRepository;
import com.buffettTest.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardLikeService {

    private final BoardLikeRepository boardLikeRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    /**
     * 좋아요 토글 (누른 적 없으면 추가, 있으면 삭제)
     */
    @Transactional
    public void toggleLike(Long userId, Long postId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        Board post = boardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (boardLikeRepository.existsByUserAndPost(user, post)) {
            // 이미 좋아요가 있다면 삭제 (취소)
            BoardLike like = BoardLike.builder().user(user).post(post).build();
            // JPA에서 복합키 엔티티 삭제 시 객체를 직접 넘기거나 ID 객체 활용
            boardLikeRepository.delete(like);
        } else {
            // 좋아요가 없다면 추가
            BoardLike like = BoardLike.builder()
                    .user(user)
                    .post(post)
                    .build();
            boardLikeRepository.save(like);
        }
    }

    /**
     * 특정 게시글의 좋아요 합계 조회
     */
    public long getLikeCount(Long postId) {
        Board post = boardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        return boardLikeRepository.countByPost(post);
    }
}