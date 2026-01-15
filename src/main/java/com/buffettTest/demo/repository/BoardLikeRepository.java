package com.buffettTest.demo.repository;

import com.buffettTest.demo.entity.Board;
import com.buffettTest.demo.entity.BoardLike;
import com.buffettTest.demo.entity.BoardLikeId;
import com.buffettTest.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardLikeRepository extends JpaRepository<BoardLike, BoardLikeId> {

    // 게시글당 좋아요 총 개수 조회
    long countByPost(Board post);

    // 특정 유저가 특정 게시글에 좋아요를 눌렀는지 확인
    boolean existsByUserAndPost(User user, Board post);
}