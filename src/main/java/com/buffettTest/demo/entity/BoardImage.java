package com.buffettTest.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "BOARD_IMAGES")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BoardImage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "IMAGE_SEQ_GEN")
    @SequenceGenerator(name = "IMAGE_SEQ_GEN", sequenceName = "IMAGE_SEQ", allocationSize = 1)
    @Column(name = "IMAGE_ID")
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POST_ID", nullable = false)
    private Board post;

    @Column(name = "IMAGE_URL", nullable = false, length = 500)
    private String imageUrl;

    @Builder.Default
    @Column(name = "SORT_ORDER")
    private Integer sortOrder = 0;
}