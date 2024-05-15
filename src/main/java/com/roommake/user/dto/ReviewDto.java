package com.roommake.user.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ReviewDto {
    private int id;             // 리뷰 번호
    private String name;        // 상품 이름
    private String imageName;   // 상품 이미지
    private int rating;         // 별점
    private Date createDate;    // 리뷰 작성일
    private String content;     // 리뷰 내용
    private int voteCount;      // 리뷰 추천수
    private String size;       // 상품 크기
    private String color;      // 상품 색상
}
