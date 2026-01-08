package com.buffettTest.demo.repository;

import com.buffettTest.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

// JpaRepository를 상속받는 것만으로도 save(), findAll(), delete() 등의 기본 메서드가 자동생성
// <User, Long>은 <연결할 엔티티, 그 엔티티의 PK 타입>을 의미한다.

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 핵심 기능 - 카카오 고유 ID로 사용자를 찾는 쿼리
    // JPA가 메서드 이름을 분석해 "select * from USERS where kakao_id = ?" 쿼리를 자동으로 만듦
    Optional<User> findByKakaoId(Long kakaoId);

    // 기능 - 닉네임 중복 체크
    boolean existsByNickname(String nickname);
}
