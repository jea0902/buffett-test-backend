package com.buffettTest.demo;

import com.buffettTest.demo.entity.User;
import com.buffettTest.demo.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest // 데이터 연동 테스트
class DemoApplicationTests {

	@Autowired // Repo를 스프링이 자동으로 가져오게 한다.
	private UserRepository userRepository;

	@Test
	void 유저저장_테스트() {
		// 1. 테스트용 유저 객체 생성(Builder 패턴)
		User user = User.builder()
				.kakaoId(12345678L)
				.nickname("워렌버핏")
				.email("test@test.com")
				.build();

		// 2. DB에 저장
		userRepository.save(user);

		System.out.println("✅ 유저 저장 성공");
	}

}
