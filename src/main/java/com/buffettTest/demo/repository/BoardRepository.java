package com.buffettTest.demo.repository;

import com.buffettTest.demo.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    // 삭제되지 않은 게시글만 최신순으로 조회
    List<Board> findByDeletedAtIsNullOrderByCreatedAtDesc();

    // 페이징 처리가 필요한 경우 (나중에 게시판 목록 만들 때 필수)
    Page<Board> findByDeletedAtIsNull(Pageable pageable);
}