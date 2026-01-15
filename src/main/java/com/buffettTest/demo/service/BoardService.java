package com.buffettTest.demo.service;

import com.buffettTest.demo.entity.Board;
import com.buffettTest.demo.entity.User;
import com.buffettTest.demo.repository.BoardRepository;
import com.buffettTest.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 작성
     */
    @Transactional
    public Long createPost(Long userId, String title, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("작성자를 찾을 수 없습니다."));

        Board board = Board.builder()
                .user(user)
                .title(title)
                .content(content)
                .build();

        return boardRepository.save(board).getPostId();
    }

    /**
     * 게시글 단건 조회 (조회수 증가 포함)
     */
    @Transactional
    public Board getPost(Long postId) {
        Board board = boardRepository.findById(postId)
                .filter(b -> b.getDeletedAt() == null) // 삭제된 글은 조회 안됨
                .orElseThrow(() -> new RuntimeException("존재하지 않거나 삭제된 게시글입니다."));

        // 조회수 증가
        board.setViewCount(board.getViewCount() + 1);

        return board;
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public void updatePost(Long postId, String title, String content) {
        Board board = boardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        board.setTitle(title);
        board.setContent(content);
        // @PreUpdate에 의해 updatedAt은 자동 갱신됨
    }

    /**
     * 게시글 삭제 (Soft Delete)
     */
    @Transactional
    public void deletePost(Long postId) {
        Board board = boardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        board.setDeletedAt(LocalDateTime.now()); // 실제 삭제 대신 시간 기록
    }
}