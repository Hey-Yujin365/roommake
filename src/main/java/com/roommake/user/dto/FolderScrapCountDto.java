package com.roommake.user.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 폴더 카테고리 내부 스크랩 개수 조회
 */
@Getter
@Setter
public class FolderScrapCountDto {
    private int productCount;      // 상품 스크랩 조회
    private int communityCount;    // 커뮤니티 스크랩 조회
    private int totalCount;        // 총 스크랩 조회
}
