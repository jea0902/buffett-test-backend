package com.buffettTest.demo.service;

import com.buffettTest.demo.entity.Board;
import com.buffettTest.demo.entity.BoardImage;
import com.buffettTest.demo.repository.BoardImageRepository;
import com.buffettTest.demo.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 이미지는 보통 게시글을 작성할 때 함께 업로드되거나, 게시글 수정 시
// 기존 이미지를 관리하는 용도로 쓰임.

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardImageService {

    private final BoardImageRepository boardImageRepository;
    private final BoardRepository boardRepository;

    /**
     * 이미지 경로 저장 (단건)
     */
    @Transactional
    public void addImage(Long postId, String imageUrl, Integer sortOrder) {
        Board post = boardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        BoardImage image = BoardImage.builder()
                .post(post)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .build();

        boardImageRepository.save(image);
    }

    /**
     * 특정 게시글의 모든 이미지 조회
     */
    public List<BoardImage> getImagesByPost(Long postId) {
        Board post = boardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        return boardImageRepository.findByPostOrderBySortOrderAsc(post);
    }

    /**
     * 이미지 삭제 (DB 정보만 삭제, 실제 S3 파일은 별도 처리 권장)
     */
    @Transactional
    public void deleteImage(Long imageId) {
        boardImageRepository.deleteById(imageId);
    }
}