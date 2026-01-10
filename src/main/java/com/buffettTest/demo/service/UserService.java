package com.buffettTest.demo.service;

import com.buffettTest.demo.entity.User;
import com.buffettTest.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // final이 붙은 변수(userRepository)를 자동으로 연결(의존성 주입)
@Transactional(readOnly = true) // 읽기 전용으로 설정하여 성능 최적화(실수방지)
public class UserService {

    // DB에 접근하기 위한 인터페이스 (타입을 UserRepository로 엄격히 제한)
    private final UserRepository userRepository;

    /**
     * 신규 회원가입 로직
     * 
     * @param user : 가입시키려는 유저의 정보가 담긴 객체 (User 타입)
     * @return : DB에 저장된 후의 유저 객체
     */
    // 관리자는 DB에서 직접 ROLE을 ADMIN으로 수정하여 관리자 계정으로 사용할 예정

    @Transactional
    public User registUser(User user) {

        // 카카오 id 중복 체크
        // + Optional은 값이 있을 수도, 없을 수도 있음을 나타내는 자바의 안전한 타입
        Optional<User> existingUser = userRepository.findByKakaoId(user.getKakaoId());
        if (existingUser.isPresent()) {
            throw new RuntimeException("이미 가입된 카카오 계정입니다."); // 에러 발생시키고 중단
        }

        // 닉네임 중복 체크 - existsBy는 결과를 true or false로 반환
        if (userRepository.existsByNickname(user.getNickname())) {
            throw new RuntimeException("이미 사용 중인 닉네임입니다.");
        }

        // 별도 설정 없어도 entity의 @Builder.default에 의해 생성됨
        // 모든 검사를 통과하면 DB에 저장하고, 저장된 데이터를 반환
        return userRepository.save(user);
    }

    /**
     * 특정 유저가 관리자인지 확인하는 로직
     * 
     * @param userId : 찾으려는 유저의 고유 번호 (Long 타입 - 큰 숫자형)
     * @return : 관리자면 true, 아니면 false
     */
    public boolean isAdmin(Long userId) {
        return userRepository.findById(userId) // ID로 유저를 찾아서
                .map(user -> "ADMIN".equals(user.getRole())) // 찾았다면 권한이 'ADMIN'인지 비교
                .orElse(false); // 유저 자체가 없으면 거짓(false) 반환
    }
}
