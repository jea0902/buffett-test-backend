package com.buffettTest.demo;

import com.buffettTest.demo.entity.User;
import com.buffettTest.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BitcosApplication { // 클래스명이 BticosApplication이면 그대로 두세요!

    public static void main(String[] args) {
        SpringApplication.run(BitcosApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository) {
        return args -> {
            // 중복 저장 방지를 위해 닉네임으로 체크
            if (!userRepository.existsByNickname("워렌버핏")) {
                User user = User.builder()
                        .kakaoId(12345678L)
                        .nickname("워렌버핏")
                        .email("test@test.com")
                        .build();

                userRepository.save(user);
                System.out.println("✅ [성공] 서버 실행과 함께 '워렌버핏' 데이터가 저장되었습니다.");
            } else {
                System.out.println("ℹ️ [확인] '워렌버핏' 데이터가 이미 DB에 있습니다.");
            }
        };
    }
}