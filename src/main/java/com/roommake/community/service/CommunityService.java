package com.roommake.community.service;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.roommake.community.dto.*;
import com.roommake.community.enums.CommStatusEnum;
import com.roommake.community.mapper.CommunityMapper;
import com.roommake.community.mapper.CommunityReplyMapper;
import com.roommake.community.vo.*;
import com.roommake.config.S3Config;
import com.roommake.dto.ListDto;
import com.roommake.dto.Pagination;
import com.roommake.user.mapper.UserMapper;
import com.roommake.user.vo.Follow;
import com.roommake.user.vo.ScrapFolder;
import com.roommake.user.vo.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private String localLocation = "C:/roommake/src/main/resources/static/images/";
    private final S3Config s3Config;

    private final CommunityMapper communityMapper;
    private final CommunityReplyMapper communityReplyMapper;
    private final UserMapper userMapper;

    /**
     * CKEditor에 업로드한 이미지를 s3에 저장한다.
     *
     * @param request
     * @return 이미지 s3Url
     * @throws IOException
     */
    public String imageUpload(MultipartRequest request) throws IOException {

        // request 이미지 파일을 얻는다.
        MultipartFile file = request.getFile("upload");

        // 이미지 파일에서 이름 및 확장자를 얻는다.
        String fileName = file.getOriginalFilename();
        String ext = fileName.substring(fileName.indexOf("."));

        // 이미지 파일 이름 유일성을 위해 uuid 생성
        String uuidFileName = UUID.randomUUID() + ext;

        // 서버환경에 저장할 경로 생성
        String localPath = localLocation + uuidFileName;

        // 서버환경에 이미지 파일을 저장
        File localFile = new File(localPath);
        file.transferTo(localFile);

        s3Config.amazonS3Client().putObject(new PutObjectRequest(bucket, uuidFileName, localFile).withCannedAcl(CannedAccessControlList.PublicRead));
        String s3Url = s3Config.amazonS3Client().getUrl(bucket, uuidFileName).toString();

        // 서버에 저장한 이미지를 삭제
        localFile.delete();

        return s3Url;
    }

    /**
     * 커뮤니티 글을 모두 조회한다.
     *
     * @param commCatId 커뮤니티 카테고리 아이디
     * @param criteria  정렬 및 페이징 정보
     * @return 커뮤니티 글 목록, 정렬 및 페이징
     */
    @Transactional(readOnly = true)
    public ListDto<Community> getAllCommunitiesByCatId(int commCatId, CommCriteria criteria) {
        criteria.setCommCatId(commCatId);
        int totalRows = communityMapper.getCommTotalRowByCommId(criteria);

        Pagination pagination = new Pagination(criteria.getPage(), totalRows, criteria.getRows());
        criteria.setBegin(pagination.getBegin());
        criteria.setEnd(pagination.getEnd());

        List<Community> houseCommList = communityMapper.getAllCommunitiesByCatId(criteria);

        return new ListDto<Community>(houseCommList, pagination);
    }

    /**
     * 커뮤니티 카테고리를 모두 조회한다.
     *
     * @return 커뮤니티 카테고리 목록
     */
    @Transactional(readOnly = true)
    public List<CommunityCategory> getAllCommCategories() {

        return communityMapper.getAllCommCategories();
    }

    /**
     * 커뮤니티 글을 등록한다.
     *
     * @param communityForm 커뮤니티 글 등록폼
     * @param imageName     이미지 이름
     * @param userId        커뮤니티 글을 작성한 유저 아이디
     */
    public void createCommunity(CommunityForm communityForm, String imageName, int userId) {
        User user = User.builder().id(userId).build();
        Community community = Community.builder()
                .category(new CommunityCategory(communityForm.getCategoryId()))
                .user(user)
                .title(communityForm.getTitle())
                .content(communityForm.getContent())
                .imageName(imageName)
                .build();
        communityMapper.createCommunity(community);
    }

    /**
     * 커뮤니티 글을 조회한다.
     *
     * @param commId 커뮤니티 글 아이디
     * @return 커뮤니티 글
     */
    @Transactional(readOnly = true)
    public Community getCommunityByCommId(int commId) {

        return communityMapper.getCommunityByCommId(commId);
    }

    /**
     * 커뮤니티 글 상세를 조회한다.
     *
     * @param commId 커뮤니티 글 아이디
     * @param email  로그인한 유저 이메일
     * @return 커뮤니티 글 상세 (커뮤니티 글, 로그인한 유저의 좋아요, 스크랩 여부, 총 댓글 갯수 등)
     */
    public CommDetailDto getCommunityDetail(int commId, String email, int replyCurrentPage) {
        // 신고 카테고리
        List<ComplaintCategory> complaintCategories = communityMapper.getComplaintCategories();
        // 커뮤니티 글, 조회수 1 증가
        Community community = communityMapper.getCommunityByCommId(commId);
        community.setViewCount(community.getViewCount() + 1);
        communityMapper.modifyCommunity(community);

        CommDetailDto commDetailDto = new CommDetailDto();
        commDetailDto.setComplaintCategories(complaintCategories);
        commDetailDto.setCommunity(community);

        // 좋아요, 스크랩, 팔로우 여부
        if (email != null) {
            User user = userMapper.getUserByEmail(email);
            CommunityLike commLikeUser = CommunityLike.builder().commId(commId).userId(user.getId()).build();
            if (communityMapper.getCommLikeUser(commLikeUser) != null) {
                commDetailDto.setLike(true);
            }

            CommunityScrap commScrapUser = CommunityScrap.builder()
                    .community(new Community(commId))
                    .user(new User(user.getId()))
                    .build();
            CommunityScrap existCommunityScrap = communityMapper.getCommScrapUser(commScrapUser);
            if (existCommunityScrap != null && "N".equals(existCommunityScrap.getDeleteYn())) {
                commDetailDto.setScrap(true);
            }

            Follow follow = new Follow(user.getId(), community.getUser().getId());
            if (userMapper.getFollow(follow) != null) {
                commDetailDto.setFollow(true);
            }
        }

        // 댓글 총 갯수, 댓글 목록, 댓글 좋아요 여부, 댓글 페이지네이션
        CommReplyListDto replyDto = getAllCommReplies(commId, email, replyCurrentPage);
        commDetailDto.setTotalReplyCount(replyDto.getTotalReplyCount());
        commDetailDto.setCommunityReplies(replyDto.getCommunityReplies());
        commDetailDto.setReplyPagination(replyDto.getPagination());

        // 다른 커뮤니티 글을 추천 목록
        List<Community> recommendCommunities = getRecommendCommunities(community);
        commDetailDto.setRecommendCommunities(recommendCommunities);

        return commDetailDto;
    }

    /**
     * 커뮤니티 글의 댓글을 모두 조회한다.
     *
     * @param commId      커뮤니티 글 번호
     * @param email       로그인한 유저 이메일
     * @param currentPage 댓글 현재 페이지
     * @return 총 댓글 갯수, 댓글 목록, 댓글 페이지네이션 정보가 들어있는 CommReplyListDto
     */
    @Transactional(readOnly = true)
    public CommReplyListDto getAllCommReplies(int commId, String email, int currentPage) {
        int totalReplyCount = communityReplyMapper.getTotalReplyCountByCommId(commId); // // 삭제된 댓글 제외

        int totalReplyRow = communityReplyMapper.getTotalReplyRow(commId); // 삭제된 댓글 (대댓글이 있는 댓글) 포함
        Pagination pagination = new Pagination(currentPage, totalReplyRow);
        CommReplyCriteria commReplyCriteria = new CommReplyCriteria();
        commReplyCriteria.setBegin(pagination.getBegin());
        commReplyCriteria.setEnd(pagination.getEnd());
        commReplyCriteria.setCommunityId(commId);

        CommReplyListDto commReplyListDto = new CommReplyListDto();
        commReplyListDto.setTotalReplyCount(totalReplyCount);
        commReplyListDto.setPagination(pagination);

        if (email != null) {
            User user = userMapper.getUserByEmail(email);
            commReplyCriteria.setUserId(user.getId());
            List<CommReplyDto> communityReplies = communityReplyMapper.getAllRepliesByUserId(commReplyCriteria);
            commReplyListDto.setCommunityReplies(communityReplies);
        } else {
            List<CommReplyDto> communityReplies = communityReplyMapper.getAllReplies(commReplyCriteria);
            commReplyListDto.setCommunityReplies(communityReplies);
        }

        return commReplyListDto;
    }

    /**
     * 커뮤니티 글 추천 목록
     *
     * @param community 커뮤니티 글
     * @return 추천 커뮤니티 글 목록 (추천 커뮤니티 글의 좋아요 많은 순서대로 4개)
     */
    @Transactional(readOnly = true)
    public List<Community> getRecommendCommunities(Community community) {
        CommCriteria commCriteria = new CommCriteria();
        commCriteria.setCommCatId(community.getCategory().getId());
        commCriteria.setCommId(community.getId());
        commCriteria.setSort("like");
        commCriteria.setBegin(1);
        commCriteria.setEnd(4);

        return communityMapper.getAllCommunitiesByCatId(commCriteria);
    }

    /**
     * 커뮤니티 글을 수정한다.
     *
     * @param communityForm 커뮤니티 글 수정폼
     * @param imageName     이미지 이름
     * @param community     커뮤니티 글
     */
    public void modifyCommunity(CommunityForm communityForm, String imageName, Community community) {
        community.setTitle(communityForm.getTitle());
        community.setContent(communityForm.getContent());
        community.setUpdateDate(new Date());
        community.setImageName(imageName);
        communityMapper.modifyCommunity(community);
    }

    /**
     * 커뮤니티 글을 삭제한다.
     *
     * @param community 커뮤니티 글
     */
    public void deleteCommunity(Community community) {
        community.setDeleteDate(new Date());
        community.setStatus(CommStatusEnum.DELETE.getStatus());
        community.setDeleteYn("Y");
        communityMapper.modifyCommunity(community);
    }

    /**
     * 커뮤니티 글 좋아요를 추가한다.
     *
     * @param commId 커뮤니티 글 아이디
     * @param userId 좋아요를 누른 유저 아이디
     * @return 커뮤니티 글 좋아요 수
     */
    public int addCommunityLike(int commId, int userId) {
        CommunityLike commLikeUser = CommunityLike.builder().commId(commId).userId(userId).build();
        communityMapper.addCommunityLike(commLikeUser);

        Community community = communityMapper.getCommunityByCommId(commId);
        community.setLikeCount(community.getLikeCount() + 1);
        communityMapper.modifyCommunity(community);

        return community.getLikeCount();
    }

    /**
     * 커뮤니티 글 좋아요를 취소한다.
     *
     * @param commId 커뮤니티 글 아이디
     * @param userId 좋아요 취소를 누른 유저 아이디
     * @return 커뮤니티 글 좋아요 수
     */
    public int deleteCommunityLike(int commId, int userId) {
        CommunityLike commLikeUser = CommunityLike.builder().commId(commId).userId(userId).build();
        communityMapper.deleteCommunityLike(commLikeUser);

        Community community = communityMapper.getCommunityByCommId(commId);
        community.setLikeCount(community.getLikeCount() - 1);
        communityMapper.modifyCommunity(community);

        return community.getLikeCount();
    }

    /**
     * 커뮤니티 글의 댓글을 등록한다.
     *
     * @param commId    커뮤니티 글 아이디
     * @param replyForm 댓글 등록폼
     * @param userId    유저 아이디
     */
    public void createCommunityReply(int commId, CommReplyForm replyForm, int userId) {
        Community community = Community.builder().id(commId).build();
        User user = User.builder().id(userId).build();
        CommunityReply communityReply = CommunityReply.builder()
                .community(community)
                .user(user)
                .content(replyForm.getContent())
                .build();
        if (replyForm.getParentsReplyId() != 0) {
            communityReply.setGroupId(replyForm.getParentsReplyId());
            communityReply.setParentsId(replyForm.getParentsReplyId());
            communityReplyMapper.createCommunityReReply(communityReply);
        } else {
            communityReplyMapper.createCommunityReply(communityReply);
            communityReply.setGroupId(communityReply.getId());
            communityReplyMapper.modifyReplyGroupId(communityReply);
        }
    }

    /**
     * 커뮤니티 글의 댓글을 조회한다.
     *
     * @param replyId 댓글 아이디
     * @return 댓글
     */
    @Transactional(readOnly = true)
    public CommunityReply getCommunityReplyByReplyId(int replyId) {

        return communityReplyMapper.getCommReplyByReplyId(replyId);
    }

    /**
     * 커뮤니티 글의 댓글을 수정한다.
     *
     * @param communityReply 수정 전 댓글
     * @param content        수정한 댓글 내용
     * @return 수정 후 댓글
     */
    public CommunityReply modifyCommunityReply(CommunityReply communityReply, String content) {
        communityReply.setContent(content);
        communityReply.setUpdateDate(new Date());
        communityReplyMapper.modifyCommunityReply(communityReply);

        return communityReply;
    }

    /**
     * 커뮤니티 글의 댓글을 삭제한다.
     *
     * @param communityReply 삭제할 댓글
     */
    public void deleteCommunityReply(CommunityReply communityReply) {
        int reReplyCount = communityReplyMapper.getReReplyCount(communityReply.getId());
        if (reReplyCount == 0) {
            communityReply.setDeleteDate(new Date());
            communityReply.setDeleteYn("Y");
        }
        communityReply.setStatus(CommStatusEnum.DELETE.getStatus());
        communityReplyMapper.modifyCommunityReply(communityReply);
    }

    /**
     * 커뮤니티 글을 신고한다.
     *
     * @param commId         커뮤니티 글 아이디
     * @param complaintCatId 신고 카테고리 번호
     * @param userId         유저 아이디
     */
    public void createCommunityComplaint(int commId, int complaintCatId, int userId) {
        Community community = communityMapper.getCommunityByCommId(commId);
        ComplaintCategory complaintCategory = ComplaintCategory.builder().id(complaintCatId).build();
        User user = User.builder().id(userId).build();
        CommunityComplaint communityComplaint = CommunityComplaint.builder()
                .community(community)
                .complaintCat(complaintCategory)
                .user(user)
                .build();
        communityMapper.createCommunityComplaint(communityComplaint);
    }

    /**
     * 커뮤니티 글의 댓글을 신고한다.
     *
     * @param replyId        댓글 아이디
     * @param complaintCatId 신고 카테고리 번호
     * @param userId         유저 아이디
     */
    public void createCommunityReplyComplaint(int replyId, int complaintCatId, int userId) {
        CommunityReply communityReply = CommunityReply.builder().id(replyId).build();
        ComplaintCategory complaintCategory = ComplaintCategory.builder().id(complaintCatId).build();
        User user = User.builder().id(userId).build();
        CommunityReplyComplaint replyComplaint = CommunityReplyComplaint.builder()
                .reply(communityReply)
                .complaintCat(complaintCategory)
                .user(user)
                .build();
        communityReplyMapper.createCommunityReplyComplaint(replyComplaint);
    }

    /**
     * 커뮤니티 글의 댓글에 좋아요를 추가한다.
     *
     * @param replyId 댓글 아이디
     * @param userId  유저 아이디
     * @return 댓글 좋아요수
     */
    public int addCommunityReplyLike(int replyId, int userId) {
        CommunityReplyLike replyLike = CommunityReplyLike.builder().commReplyId(replyId).userId(userId).build();
        communityReplyMapper.addCommunityReplyLike(replyLike);
        CommunityReply reply = communityReplyMapper.getCommReplyByReplyId(replyId);
        reply.setLikeCount(reply.getLikeCount() + 1);
        communityReplyMapper.modifyCommunityReply(reply);

        return reply.getLikeCount();
    }

    /**
     * 커뮤니티 글의 댓글에 좋아요를 삭제(취소)한다.
     *
     * @param replyId 댓글 아이디
     * @param userId  유저 아이디
     * @return 댓글 좋아요 수
     */
    public int deleteCommunityReplyLike(int replyId, int userId) {
        CommunityReplyLike replyLike = CommunityReplyLike.builder().commReplyId(replyId).userId(userId).build();
        communityReplyMapper.deleteCommunityReplyLike(replyLike);
        CommunityReply reply = communityReplyMapper.getCommReplyByReplyId(replyId);
        reply.setLikeCount(reply.getLikeCount() - 1);
        communityReplyMapper.modifyCommunityReply(reply);

        return reply.getLikeCount();
    }

    /**
     * 모든 스크랩 폴더를 조회한다.
     *
     * @param userId 유저 아이디
     * @return 스크랩 폴더 목록
     */
    @Transactional(readOnly = true)
    public List<ScrapFolder> getScrapFolders(int userId) {

        return communityMapper.getAllScrapFoldersUserId(userId);
    }

    /**
     * 커뮤니티글 스크랩을 추가한다.
     *
     * @param communityId   커뮤니티글 아이디
     * @param scrapFolderId 저장할 스크랩 폴더 아이디
     * @param userId        유저 아이디
     * @return 커뮤니티글 스크랩 수
     */
    public int createCommunityScrap(int communityId, int scrapFolderId, int userId) {
        CommunityScrap communityScrap = CommunityScrap.builder()
                .community(new Community(communityId))
                .scrapFolder(new ScrapFolder(scrapFolderId))
                .user(new User(userId))
                .build();

        CommunityScrap existCommunityScrap = communityMapper.getCommScrapUser(communityScrap);
        if (existCommunityScrap != null) {
            existCommunityScrap.setUpdateDate(new Date());
            existCommunityScrap.setDeleteYn("N");
            communityMapper.modifyCommunityScrap(existCommunityScrap);
        } else {
            communityMapper.createCommunityScrap(communityScrap);
        }

        Community community = communityMapper.getCommunityByCommId(communityId);
        community.setScrapCount(community.getScrapCount() + 1);
        communityMapper.modifyCommunity(community);

        return community.getScrapCount();
    }

    /**
     * 커뮤니티글 스크랩을 삭제한다.
     *
     * @param communityId 커뮤니티글 아이디
     * @param userId      유저 아이디
     * @return 커뮤니티글 스크랩 수
     */
    public int deleteCommunityScrap(int communityId, int userId) {
        CommunityScrap communityScrap = CommunityScrap.builder()
                .community(new Community(communityId))
                .user(new User(userId))
                .build();
        CommunityScrap existCommunityScrap = communityMapper.getCommScrapUser(communityScrap);

        existCommunityScrap.setDeleteDate(new Date());
        existCommunityScrap.setDeleteYn("Y");
        communityMapper.modifyCommunityScrap(existCommunityScrap);

        Community community = communityMapper.getCommunityByCommId(communityId);
        community.setScrapCount(community.getScrapCount() - 1);
        communityMapper.modifyCommunity(community);

        return community.getScrapCount();
    }

    /**
     * 유저 ID로 커뮤니티 정보 조회
     *
     * @param userId 유저 아이디
     * @param page   페이징
     * @return 마이페이지 커뮤니티 정보
     */
    public List<MyPageCommunity> getCommunitiesByUserId(int userId, int page) {
        int offset = (page - 1) * 5;

        return communityMapper.getCommunitiesByUserId(userId, offset);
    }

    /**
     * 유저 ID로 커뮤니티 개수 조회
     *
     * @param userId 유저 아이디
     * @return 커뮤니티 개수
     */
    public int getTotalRows(int userId) {
        return communityMapper.getTotalRowsByUserId(userId);
    }

    /**
     * 유저 아이디로 사용자가 작성한 총 게시물 수 조회
     *
     * @param userId 유저 아이디
     * @return 총 게시물 수
     */
    public int countCommunitiesByUserId(int userId) {
        return communityMapper.countCommunitiesByUserId(userId);
    }
}
