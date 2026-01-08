package com.buffettTest.demo.entity;

import jakarta.persistence.*; // DB와 자바 객체를 연결하는 JPA 도구 모음
import lombok.*; // 코드를 간결하게
import java.time.LocalDateTime;

@Entity
@Table(name = "USERS")
@Getter // 모든 필드의 값을 읽을 수 있는 메서드를 자동으로 만듦
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 생성자(JPA 필수)
@AllArgsConstructor // 모든 필드를 포함하는 생성자
@Builder // 복잡한 객체를 안전하고 편리하게 생성할 수 있게
public class User {

    @Id // 테이블의 PK
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "USER_SEQ_GEN")
    @SequenceGenerator(name = "USER_SEQ_GEN", sequenceName = "USER_SEQ", allocationSize = 1)
    // 위 설정은 오라클의 "USER_SEQ" 시퀀스를 사용해 번호를 자동으로 따오겠다는 뜻
    @Column(name = "USER_ID") // (name = DB는 언더바를 사용하고, 자바는 보통 카멜 케이스를 쓰니까)
    private Long userId;

    @Column(name = "KAKAO_ID", nullable = false, unique = true) // null x, 중복x
    private Long kakaoId; // 카카오 고유id

    @Column(length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 50) // 글자 수 50제한
    private String nickname;

    @Column(name = "PROFILE_IMAGE", length = 500)
    private String profileImg; // 프로필 이미지 URL 주소

    @Builder.Default // default 값 작동되게
    @Column(name = "CASH_BALANCE")
    private Long cashBalance = 100000000L; // 초기 1억 설정

    @Builder.Default
    @Column(length = 20)
    private String role = "USER"; // 권한 (USER, ADMIN)

    @Builder.Default
    @Column(name = "ALLOW_NOTIF_TRADE", length = 1)
    private String allowNotifTrade = "Y"; // 거래 알림 수신 여부 (Y/N)

    @Builder.Default
    @Column(name = "ALLOW_NOTIF_BOARD", length = 1)
    private String allowNotifBoard = "Y"; // 게시판 알림 수신 여부 (Y/N)

    @Builder.Default
    @Column(name = "LAST_RESET_DATE")
    private LocalDateTime lastResetDate = LocalDateTime.now(); // 잔고 초기화 기준일

    @Builder.Default
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 가입일

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt; // 탈퇴일 (null이면 활동 중)

}
