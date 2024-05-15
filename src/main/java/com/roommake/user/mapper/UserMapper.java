package com.roommake.user.mapper;

import com.roommake.dto.Criteria;
import com.roommake.user.dto.*;
import com.roommake.user.vo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    // 이메일로 사용자 및 권한 조회
    Map<String, Object> getUserByEmailWithRoles(String email);

    // 이메일로 유저 조회
    User getUserByEmail(String email);

    // 유저 등록
    void createUser(User user);

    // 닉네임으로 유저 조회
    User getUserByNickname(String nickname);

    // 추천인으로 유저 조회
    User getUserByRecommendCode(String recommendCode);

    // 아이디로 유저 조회
    User getUserById(int userId);

    // 추천인 코드 중복 여부 확인
    boolean existRecommendCode(String uniqueRecommendCode);

    // 모든 약관 조회
    List<Term> getAllTerms();

    // 유저 약관 동의 저장
    void createTermAgreement(TermAgreement termAgreement);

    // 약관 ID로 약관 정보를 조회
    Term getTermById(int id);

    // 추천인 코드로 유저 검색
    User getByRecommendCode(String recommendCode);

    // 신규 가입자에게 포인트 적립
    void addPlusPointForNewUser(PlusPointHistory plusPointHistory);

    // 기존 회원에게 포인트 적립
    void addPlusPointForExistUser(PlusPointHistory plusPointHistory);

    // 기존 회원 포인트 적립내역 업데이트
    void modifyUserPoint(int id, int point);

    // 신규 회원 포인트 적립내역 업데이트
    void modifyNewUserPoint(int id, int point);

    // 이메일 중복 여부 확인
    boolean ExistEmail(String email);

    // 유저 회원정보 수정
    void modifyUser(User user);

    // 조건에 맞는 유저 수 조회
    int getTotalRows(Criteria criteria);

    // 기준에 맞는 유저 목록 조회(페이징, 정렬, 검색)
    List<User> getUsers(Criteria criteria);

    // 유저의 모든 스크랩 조회
    List<AllScrap> getAllScraps(int userId, int offset);

    // 폴더별 유저의 모든 스크랩 조회
    List<AllScrap> getAllScrapsByFolderId(int userId, int folderId, int offset);

    // 폴더별 스크랩 행 조회
    FolderScrapCountDto getAllScrapsByFolderIdRows(int folderId, int userId);

    // 모든 폴더 조회
    List<AllScrap> getScrapFolders(int id, int offset);

    // 유저의 모든 상품 스크랩 개수
    int getScrapFoldersRows(int id);

    // 유저의 모든 상품 스크랩 조회
    List<UserProductScrap> getProductScraps(int userId, int catId, int offset);

    // 유저의 모든 상품 스크랩 개수
    int getProductRows(int userId, int catId);

    // 유저의 모든 커뮤니티 스크랩 조회
    List<UserCommScrap> getCommunityScraps(int id, int catId, int offset);

    // 커뮤니티 스크랩 행 조회
    int getCommunityScrapRows(int id, int catId);

    // 상품 스크랩을 기본 폴더로 이동
    void modifyProductScrapToDefaultFolder(int userId, int folderId);

    // 커뮤니티 스크랩을 기본 폴더로 이동
    void modifyCommunityScrapToDefaultFolder(int userId, int folderId);

    // 스크랩 폴더 삭제
    void deleteScrapFolder(int userId, int folderId);

    // 특정 아이템을 다른 폴더로 이동
    void modifyScrapItemToFolder(int itemId, int userId, int targetFolderId, String type);

    // 특정 아이템을 삭제
    void deleteScrapItem(int itemId, int userId, String type);

    // 스크랩 폴더 이름 및 설명 수정
    void modifyScrapFolder(Map<String, Object> params);

    // 새로운 스크랩 폴더 추가
    void addScrapFolder(Map<String, Object> params);

    // 유저 신고 카운트 누적
    void modifyUserComplaintCount(User user);

    // 회원가입시 기본폴더 생성
    void createDefaultFolder(int userId);

    // 새 폴더 생성 후 ID를 반환
    void addScrapFolderReturningId(Map<String, Object> params);

    // 유저별 포인트 히스토리 내역
    List<PointHistoryDto> getPointHistoryByUserId(@Param("userId") int id,
                                                  @Param("start") int start);

    // 유저별 포인트 잔액
    int getPointBalanceByUserId(int userId);

    // 유저별 포인트 히스토리 총 개수
    int getTotalPointHistory(int userId);

    // 회원탈퇴 처리
    void deleteUser(String email, String status, int point);

    Follow getFollow(Follow follow);

    void addFollow(Follow follow);

    void deleteFollow(Follow follow);

    // 유저의 모든 스크랩수 조회
    int getTotalScrapCount(int userId);

    // 유저의 모든 좋아요 조회
    List<LikeDto> getUserLikes(int userId);

    // 유저의 모든 좋아요 개수 조회
    int getTotalLikes(int userId);

    // 유저 ID로 암호화된 비밀번호 가져오기
    String getPassword(String userId);

    // 유저 비밀번호 업데이트
    int updatePassword(Map<String, String> params);

    // 모든 스크랩 행 조회
    int getAllScrapsRows(int userId);

    // 유저의 리뷰 베스트 순 조회
    List<ReviewDto> getReviewsByBest(int userId);

    // 유저의 리뷰 최신순 조회
    List<ReviewDto> getReviewsByRecent(int userId);

    // 리뷰 삭제
    void deleteReview(int reviewId, int userId);

    // 리뷰 행 조회
    int getReviewRows(int userId);

    // 유저의 모든 스크랩 조회
    List<ReviewDto> getReviewsByBest(int userId, int offset);

    // 유저의 모든 스크랩 조회
    List<ReviewDto> getReviewsByRecent(int userId, int offset);

    // 팔로잉 수 증가
    void addFollowingCount(int followerUserId);

    // 팔로워 수 증가
    void addFollowerCount(int followeeUserId);

    // 팔로잉 수 감소
    void deleteFollowingCount(int followerUserId);

    // 팔로워 수 감소
    void deleteFollowerCount(int followeeUserId);

    // 유저의 스크랩 개수 조회
    List<ScrapCountDto> getScrapCount(int userId);
}

