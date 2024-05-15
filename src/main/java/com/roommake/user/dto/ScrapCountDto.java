package com.roommake.user.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 스크랩 카테고리 별 개수 조회 DTO
 */
@Getter
@Setter
public class ScrapCountDto {
    private int communityCount;     // 커뮤니티 스크랩 개수
    private int productCount;       // 상품 스크랩 개수
    private int totalCount;         // 총 스크랩 개수
    private int folderCount;        // 폴더 개수
}
