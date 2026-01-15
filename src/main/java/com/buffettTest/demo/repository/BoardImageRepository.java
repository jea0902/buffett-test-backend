package com.buffettTest.demo.repository;

import com.buffettTest.demo.entity.Board;
import com.buffettTest.demo.entity.BoardImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BoardImageRepository extends JpaRepository<BoardImage, Long> {

    // 특정 게시글의 이미지를 정렬 순서대로 조회
    List<BoardImage> findByPostOrderBySortOrderAsc(Board post);
}