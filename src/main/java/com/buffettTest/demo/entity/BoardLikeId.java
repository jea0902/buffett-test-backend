package com.buffettTest.demo.entity;

import java.io.Serializable;
import lombok.*;

// 복합키 식별자 클래스
// 복합키를 사용할 때는 두 ID를 하나로 묶어줄 별도의 클래스가 필요

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // 복합키 비교를 위해 필수
public class BoardLikeId implements Serializable {
    private Long user; // BoardLike 엔티티의 필드명과 일치해야 함
    private Long post;
}