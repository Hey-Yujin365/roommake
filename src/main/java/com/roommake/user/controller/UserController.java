package com.roommake.user.controller;

import com.roommake.admin.management.service.QnaService;
import com.roommake.admin.management.vo.Qna;
import com.roommake.community.dto.MyPageCommunity;
import com.roommake.community.service.CommunityService;
import com.roommake.community.vo.CommunityCategory;
import com.roommake.dto.Message;
import com.roommake.dto.Pagination;
import com.roommake.product.service.ProductService;
import com.roommake.product.vo.ProductCategory;
import com.roommake.resolver.Login;
import com.roommake.user.dto.*;
import com.roommake.user.exception.AlreadyUsedEmailException;
import com.roommake.user.exception.AlreadyUsedNicknameException;
import com.roommake.user.mapper.UserMapper;
import com.roommake.user.security.LoginUser;
import com.roommake.user.service.UserService;
import com.roommake.user.vo.Term;
import com.roommake.user.vo.TermAgreement;
import com.roommake.user.vo.User;
import com.roommake.utils.S3Uploader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User API", description = "로그인, 회원가입, 마이페이지 API를 제공한다.")
public class UserController {

    private final UserService userService;
    private final CommunityService communityService;
    private final S3Uploader s3Uploader;
    private final ProductService productService;
    private final QnaService qnaService;
    private final UserMapper userMapper;

    @Operation(summary = "로그인 폼", description = "로그인 폼을 조회한다.")
    @GetMapping("/login")
    public String loginform(@RequestParam(value = "error", required = false) String error, Model model) {
        if ("fail".equals(error)) {
            model.addAttribute("loginError", "이메일 또는 비밀번호를 확인해 주세요.");
        }
        return "/user/loginform"; // 로그인 페이지의 뷰 이름
    }

    @Operation(summary = "회원가입 폼", description = "회원가입 폼을 조회한다.")
    @GetMapping("/signup")
    public String sinupform(Model model) {
        List<Term> terms = userService.getAllTerms();
        model.addAttribute("userSignupForm", new UserSignupForm());
        model.addAttribute("terms", terms);
        return "/user/signupform";
    }

    @Operation(summary = "회원가입 등록", description = "신규 회원을 등록한다.")
    @PostMapping("/signup")
    @Transactional
    public String signup(@Valid UserSignupForm form, BindingResult errors, RedirectAttributes redirectAttributes) {

        if (errors.hasErrors()) {
            return "/user/signupform";
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "error.confirmPassword", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            return "/user/signupform";
        }

        if (!userService.isNicknameUnique(form.getNickname())) {
            errors.rejectValue("nickname", "error.nickname", "이미 사용중인 닉네임입니다.");
            return "/user/signupform";
        }

        if (!userService.isRecommendCodeValid(form.getOptionRecommendCode())) {
            errors.rejectValue("optionRecommendCode", "optionRecommendCode", "유효하지 않은 코드입니다.");
            return "/user/signupform";
        }

        String email = form.getEmail1() + "@" + form.getEmail2();

        if (!userService.isEmailAvailable(email)) {
            errors.rejectValue("email1", "email1", "이미 가입된 계정입니다.");
            return "/user/signupform";
        }

        TermAgreement termAgreement = new TermAgreement();
        termAgreement.setAgree1(form.getTermAgreements1());
        termAgreement.setAgree2(form.getTermAgreements2());
        termAgreement.setAgree3(form.getTermAgreements3());

        if (form.getTermAgreements1() == null || form.getTermAgreements2() == null) {
            errors.reject("termAgreement", "모든 필수 약관에 동의해야 합니다.");
            return "/user/signupform";
        }

        try {
            User user = userService.createUser(form);
            // 약관 동의 정보 저장
            termAgreement.setUser(user);
            userService.agreeToTerms(termAgreement);

            // 기본 폴더 생성
            userService.createDefaultFolder(user.getId());

            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/user/login";
        } catch (AlreadyUsedEmailException ex) {
            errors.rejectValue("email", null, ex.getMessage());
            return "/user/signupform";
        } catch (Exception ex) {
            ex.printStackTrace();
            return "/user/default-error";
        }
    }

    @Operation(summary = "닉네임 중복확인", description = "닉네임 중복을 확인하는 엔드포인트")
    @ResponseBody
    @PostMapping("/check-duplicate-nickname")
    public ResponseEntity<?> checkDuplicateNickname(@RequestParam String nickname) {
        try {
            boolean isDuplicate = userService.isNicknameUnique(nickname);
            return ResponseEntity.ok(isDuplicate);
        } catch (AlreadyUsedNicknameException e) {
            // 이미 사용된 닉네임인 경우
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용 중인 닉네임입니다.");
        } catch (Exception e) {
            // 그 외 예외 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("닉네임 중복 검사 중 오류가 발생했습니다.");
        }
    }

    @Operation(summary = "추천인 코드 유효성 확인", description = "입력한 추천인 코드가 유효한지 확인하는 엔드포인트")
    @ResponseBody
    @PostMapping("/check-recommend-code")
    public ResponseEntity<?> checkRecommendCode(@RequestParam String recommendCode) {
        try {
            boolean isDuplicate = userService.isRecommendCodeValid(recommendCode);
            return ResponseEntity.ok(isDuplicate);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("추천인 코드 검사 중 오류가 발생했습니다.");
        }
    }

    @Operation(summary = "약관 조회", description = "약관을 조회한다.")
    @GetMapping("/terms/{id}")
    public String viewTerm(@PathVariable int id, Model model) {
        Term term = userService.getTermById(id);
        List<String> contentLines = Arrays.asList(term.getContent().split("\\r?\\n"));
        boolean required = term.getRequireYn().equals("Y");
        model.addAttribute("term", term);
        model.addAttribute("contentLines", contentLines);
        model.addAttribute("required", required);

        switch (id) {
            case 1:
                return "user/term1";
            case 2:
                return "user/term2";
            case 3:
                return "user/term3";
            default:
                return "user/default-error";
        }
    }

    @Operation(summary = "비밀 번호 재설정 폼", description = "비밀번호 재설정 페이지를 조회한다.")
    @GetMapping("/resetpwd")
    public String resetpassword() {
        return "user/reset-password";
    }

    @Operation(summary = "비밀 번호 재설정", description = "비밀번호 재설정 처리한다.")
    @PostMapping("/resetpwd")
    public String resetpassword(@Valid @ModelAttribute ResetPasswordForm form, BindingResult errors, Principal principal) {
        String email = form.getEmail();
        User user = userService.getUserByEmail(email);
        String userId = String.valueOf(user.getId());

        // 서버 측 유효성 검사
        if (errors.hasErrors()) {
            return "user/resetpwd";
        }

        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "error.confirmPassword", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            return "user/resetpwd";
        }
        // 새 비밀번호 업데이트
        boolean success = userService.updatePassword(userId, form.getNewPassword());
        if (success) {
            return "redirect:http://43.202.27.255/";
        } else {
            // 업데이트 실패 시 처리
            return "redirect:/user/resetpwd";
        }
    }

    @Operation(summary = "마이페이지 메인", description = "마이페이지 메인을 조회한다.")
    @GetMapping("/mypage")
    @PreAuthorize("isAuthenticated()")
    public String myPage(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                         Principal principal, Model model) {

        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        model.addAttribute("user", user);

        int userId = user.getId();
        int totalRows = communityService.getTotalRows(userId);

        // 사용자가 등록한 게시물 목록을 조회
        List<MyPageCommunity> communities = communityService.getCommunitiesByUserId(userId, page);
        model.addAttribute("communities", communities);

        // 사용자가 작성한 커뮤니티 게시글의 총 개수를 조회
        int communityCount = communityService.countCommunitiesByUserId(userId);
        model.addAttribute("communityCount", communityCount);

        // 모든 스크랩 조회
        List<AllScrap> allScraps = userService.getAllScraps(userId, 1);
        model.addAttribute("allScraps", allScraps);

        Pagination pagination = new Pagination(page, totalRows, 5);
        model.addAttribute("paging", pagination);

        int totalScrapCount = userService.getTotalScrapCount(userId);
        model.addAttribute("totalScrapCount", totalScrapCount);

        int totalLikes = userService.getTotalLikes(userId);
        model.addAttribute("totalLikes", totalLikes);

        // 마이페이지의 뷰 반환
        return "user/mypage-main";
    }

    @Operation(summary = "마이페이지 커뮤니티", description = "마이페이지 커뮤니티 조회한다.")
    @GetMapping("/mycomm")
    @PreAuthorize("isAuthenticated()")
    public String mypage2(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                          Principal principal, Model model) {

        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        model.addAttribute("user", user);

        int userId = user.getId();

        int totalRows = communityService.getTotalRows(userId);

        Pagination pagination = new Pagination(page, totalRows, 5);
        model.addAttribute("paging", pagination);
        // 사용자가 등록한 게시물 목록을 조회
        List<MyPageCommunity> communities = communityService.getCommunitiesByUserId(userId, page);
        model.addAttribute("communities", communities);

        // 사용자가 작성한 커뮤니티 게시글의 총 개수를 조회
        int communityCount = communityService.countCommunitiesByUserId(userId);
        model.addAttribute("communityCount", communityCount);

        int totalScrapCount = userService.getTotalScrapCount(userId);
        model.addAttribute("totalScrapCount", totalScrapCount);

        int totalLikes = userService.getTotalLikes(userId);
        model.addAttribute("totalLikes", totalLikes);

        return "user/mypage-community";
    }

    @Operation(summary = "스크랩 북(모두) 조회", description = "마이페이지 스크랩북 '모두' 카테고리를 조회한다.")
    @GetMapping("/scrapbook")
    public String scrapbook(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                            Principal principal, Model model) {
        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        model.addAttribute("user", user);
        int userId = user.getId();

        int totalRows = userService.getAllScrapsRows(userId);
        // 모든 스크랩 조회
        List<AllScrap> allScraps = userService.getAllScraps(userId, page);
        // 페이징
        Pagination pagination = new Pagination(page, totalRows, 30);

        model.addAttribute("allScraps", allScraps);
        model.addAttribute("paging", pagination);

        // 모든 폴더 조회
        List<AllScrap> recentScraps = userService.getScrapFolders(userId, page);
        model.addAttribute("recentScraps", recentScraps);

        // 모든 스크랩 개수 조회
        List<ScrapCountDto> scrapCounts = userService.getScrapCount(userId);
        model.addAttribute("scrapCounts", scrapCounts);

        return "user/mypage-scrapbook";
    }

    @Operation(summary = "스크랩 북(폴더) 조회", description = "마이페이지 스크랩 '폴더' 카테고리를 조회한다.")
    @GetMapping("/scrapbook1")
    public String scrapbookWithAllScraps(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                                         Principal principal, Model model) {
        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        model.addAttribute("user", user);
        int id = user.getId();

        int totalRows = userService.getScrapFoldersRows(id);

        // 모든 스크랩 조회
        List<AllScrap> allScraps = userService.getAllScraps(id, 1);

        // 폴더별 모든 스크랩 조회
        List<AllScrap> recentScraps = userService.getScrapFolders(id, page);

        // 모든 스크랩 개수 조회
        List<ScrapCountDto> scrapCounts = userService.getScrapCount(id);
        model.addAttribute("scrapCounts", scrapCounts);

        // 화면에 표시할 페이징 정보
        Pagination pagination = new Pagination(page, totalRows, 30);
        model.addAttribute("paging", pagination);

        // 기본 폴더 식별
        AllScrap defaultFolder = recentScraps.stream()
                .filter(folder -> "기본 폴더".equals(folder.getFolderName()))
                .findFirst()
                .orElse(null);
        int defaultFolderId = defaultFolder != null ? defaultFolder.getFolderId() : -1;

        // 모델에 데이터 추가
        model.addAttribute("allScraps", allScraps);
        model.addAttribute("recentScraps", recentScraps);
        model.addAttribute("defaultFolderId", defaultFolderId);

        // 스크랩북 페이지로 이동
        return "user/mypage-scrapbook1";
    }

    @Operation(summary = "스크랩 폴더 상세", description = "마이페이지 스크랩 폴더 상세를 조회한다.")
    @GetMapping("/scrapbook1/{folderId}")
    public String getAllScrapsByFolder(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                                       @PathVariable("folderId") int folderId, Principal principal, Model model) {
        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        int userId = user.getId();

        FolderScrapCountDto dto = userService.getAllScrapsByFolderIdRows(folderId, userId);
        Pagination pagination = new Pagination(page, dto.getTotalCount(), 30);
        model.addAttribute("paging", pagination);

        // 특정 폴더에 속한 모든 스크랩 조회
        List<AllScrap> allScraps = userService.getAllScrapsByFolderId(userId, folderId, page);

        // 전체 폴더 목록 조회
        List<AllScrap> allFolders = userService.getScrapFolders(userId, page);

        // 기본 폴더 식별
        List<AllScrap> recentScraps = userService.getScrapFolders(userId, page);
        AllScrap defaultFolder = recentScraps.stream()
                .filter(folder -> "기본 폴더".equals(folder.getFolderName()))
                .findFirst()
                .orElse(null);
        boolean isDefaultFolder = (defaultFolder != null && folderId == defaultFolder.getFolderId());

        // Folder 타입은 제외하고 실제 스크랩만 필터링
        List<AllScrap> actualScraps = allScraps.stream()
                .filter(scrap -> !"Folder".equals(scrap.getType()))
                .collect(Collectors.toList());

        // 상품과 커뮤니티로 각각 필터링
        List<AllScrap> productScraps = actualScraps.stream()
                .filter(scrap -> "Product".equals(scrap.getType()))
                .collect(Collectors.toList());

        List<AllScrap> communityScraps = actualScraps.stream()
                .filter(scrap -> "Community".equals(scrap.getType()))
                .collect(Collectors.toList());

        // 모델에 추가
        model.addAttribute("isDefaultFolder", isDefaultFolder);
        model.addAttribute("allFolders", allFolders);
        model.addAttribute("scraps", allScraps);
        model.addAttribute("productScraps", productScraps);
        model.addAttribute("communityScraps", communityScraps);
        model.addAttribute("productCount", dto.getProductCount());
        model.addAttribute("communityCount", dto.getCommunityCount());
        model.addAttribute("totalScrapCount", dto.getTotalCount());
        model.addAttribute("folderId", folderId);
        model.addAttribute("user", user);

        return "user/mypage-scrapbook-folder";
    }

    @Operation(summary = "스크랩 북(상품) 조회", description = "마이페이지 스크랩 '상품' 카테고리를 조회한다.")
    @GetMapping("/scrapbook2")
    public String scrapbook2(@RequestParam(name = "catId", required = false, defaultValue = "1") int catId,
                             @RequestParam(name = "page", required = false, defaultValue = "1") int page,
                             Principal principal, Model model) {
        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        model.addAttribute("user", user);
        int userId = user.getId();

        int totalRows = userService.getProductRows(userId, catId);
        List<UserProductScrap> scrappedProducts = userService.getProductScraps(userId, catId, page);
        List<ProductCategory> categories = productService.getProductMainCategories();
        List<AllScrap> allFolders = userService.getScrapFolders(userId, 1);

        Pagination pagination = new Pagination(page, totalRows, 30);

        // 모든 스크랩 개수 조회
        List<ScrapCountDto> scrapCounts = userService.getScrapCount(userId);
        model.addAttribute("scrapCounts", scrapCounts);

        model.addAttribute("categoryId", catId);
        model.addAttribute("scrappedProducts", scrappedProducts);
        model.addAttribute("categories", categories);
        model.addAttribute("allFolders", allFolders);
        model.addAttribute("paging", pagination);

        return "user/mypage-scrapbook2";
    }

    @Operation(summary = "스크랩 북(커뮤니티) 조회", description = "마이페이지 스크랩 '커뮤니티' 카테고리를 조회한다.")
    @GetMapping("/scrapbook3")
    public String scrapbook3(@RequestParam(name = "catId", required = false, defaultValue = "1") int catId,
                             @RequestParam(name = "page", required = false, defaultValue = "1") int page,
                             Principal principal, Model model) {
        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        model.addAttribute("user", user);
        int id = user.getId();

        int totalRows = userService.getCommunityScrapRows(id, catId);
        List<UserCommScrap> scrappedCommunities = userService.getCommunityScraps(id, catId, page);
        List<CommunityCategory> categories = communityService.getAllCommCategories();
        List<AllScrap> allFolders = userService.getScrapFolders(id, 1);

        Pagination pagination = new Pagination(page, totalRows, 30);

        // 모든 스크랩 개수 조회
        List<ScrapCountDto> scrapCounts = userService.getScrapCount(id);
        model.addAttribute("scrapCounts", scrapCounts);

        model.addAttribute("categoryId", catId);
        model.addAttribute("scrappedCommunities", scrappedCommunities);
        model.addAttribute("categories", categories);
        model.addAttribute("allFolders", allFolders);
        model.addAttribute("paging", pagination);

        return "user/mypage-scrapbook3";
    }

    @Operation(summary = "스크랩 폴더 삭제", description = "스크랩 폴더들을 삭제한다.")
    @PostMapping("/scrapbook1/deleteFolders")
    public String deleteScrapFolders(@RequestParam("folderIds") String folderIds, Principal principal) {
        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        int userId = user.getId();

        // 쉼표로 구분된 폴더 ID들을 리스트로 변환
        List<Integer> folderIdList = Arrays.stream(folderIds.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        // 각 폴더에 대해 삭제 및 이동 처리
        for (int folderId : folderIdList) {
            userService.deleteAndMoveScrapFolder(userId, folderId);
        }

        return "redirect:/user/scrapbook1";
    }

    @Operation(summary = "스크랩 아이템 이동", description = "선택된 스크랩 아이템을 선택된 폴더로 이동시킨다.")
    @PostMapping("/scrapbook/moveItems")
    public String moveScrapItems(
            @RequestParam("itemIds") String itemIdsCsv,
            @RequestParam("targetFolderId") int folderId,
            @RequestParam("types") String typesCsv,
            Principal principal) {
        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        int userId = user.getId();

        // 콤마로 구분된 문자열을 개별 요소의 리스트로 분할
        String[] itemIds = itemIdsCsv.split(",");
        String[] types = typesCsv.split(",");

        for (int i = 0; i < itemIds.length; i++) {
            int itemId = Integer.parseInt(itemIds[i]);
            String type = types[i];
            userService.modifyScrapItemToFolder(userId, itemId, folderId, type);
        }

        return "redirect:/user/scrapbook";
    }

    @Operation(summary = "스크랩 아이템 삭제", description = "선택된 스크랩 아이템을 삭제한다.")
    @PostMapping("/scrapbook/deleteItems")
    public String deleteScrapItems(
            @RequestParam("itemIds") String itemIdsCsv,
            @RequestParam("types") String typesCsv,
            Principal principal) {
        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        int userId = user.getId();

        String[] itemIds = itemIdsCsv.split(",");
        String[] types = typesCsv.split(",");

        for (int i = 0; i < itemIds.length; i++) {
            int itemId = Integer.parseInt(itemIds[i]);
            String type = types[i];
            userService.deleteScrapItem(userId, itemId, type);
        }

        return "redirect:/user/scrapbook";
    }

    @Operation(summary = "스크랩 폴더 수정", description = "스크랩 폴더명 및 설명을 수정한다.")
    @PostMapping("/scrapbook1/updateFolder")
    public ResponseEntity<String> modifyScrapFolder(
            @Valid @ModelAttribute FolderEdit folderEdit,
            @RequestParam("folderId") int folderId,
            Principal principal) {

        // 현재 사용자의 이메일로 유저 ID 가져오기
        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        int userId = user.getId();

        // 폴더 수정 서비스 호출
        userService.modifyScrapFolder(folderId, userId, folderEdit.getFolderName(), folderEdit.getFolderDescription());
        return ResponseEntity.ok("폴더가 수정되었습니다.");
    }

    @Operation(summary = "스크랩 폴더 추가", description = "새로운 스크랩 폴더를 생성한다.")
    @PostMapping("/scrapbook1/insertFolder")
    public ResponseEntity<String> addScrapFolder(
            @RequestParam("folderName") String folderName,
            @RequestParam("folderDescription") String folderDescription,
            Principal principal) {

        // 현재 사용자의 이메일로 유저 ID 가져오기
        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        int userId = user.getId();

        // 새로운 폴더 추가 서비스 호출
        userService.addScrapFolder(userId, folderName, folderDescription);
        return ResponseEntity.ok("새로운 폴더가 추가되었습니다.");
    }

    @Operation(summary = "새 폴더 생성 및 스크랩 아이템 이동", description = "새 폴더를 생성하고 아이템을 이동시킨다.")
    @PostMapping("/scrapbook/insertAndMove")
    public String addFolderAndMoveItems(
            @RequestParam("folderName") String folderName,
            @RequestParam("itemIds") String itemIdsCsv,
            @RequestParam("types") String itemTypesCsv,
            Principal principal) {
        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        int userId = user.getId();

        // 새로운 폴더 생성 후 ID 가져오기
        Integer newFolderId = userService.addScrapFolderReturningId(userId, folderName);

        // 콤마로 구분된 문자열을 개별 요소의 리스트로 분할
        String[] itemIds = itemIdsCsv.split(",");
        String[] itemTypes = itemTypesCsv.split(",");

        // 각 아이템을 새 폴더로 이동
        for (int i = 0; i < itemIds.length; i++) {
            int itemId = Integer.parseInt(itemIds[i]);
            String itemType = itemTypes[i];

            // 이동 로직 수행
            userService.modifyScrapItemToFolder(userId, itemId, newFolderId, itemType);
        }

        // 새 폴더 페이지로 리다이렉트
        return "redirect:/user/scrapbook1/" + newFolderId;
    }

    @Operation(summary = "마이페이지 좋아요 조회", description = "마이페이지 좋아요 페이지를 조회한다.")
    @GetMapping("/heart")
    public String heart(Model model, Principal principal) {
        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        int userId = user.getId();

        // 유저의 모든 좋아요 조회
        List<LikeDto> userLikes = userService.getUserLikes(userId);
        model.addAttribute("userLikes", userLikes);
        model.addAttribute("user", user);

        int totalScrapCount = userService.getTotalScrapCount(userId);
        model.addAttribute("totalScrapCount", totalScrapCount);

        int totalLikes = userService.getTotalLikes(userId);
        model.addAttribute("totalLikes", totalLikes);

        return "user/mypage-heart";
    }

    @Operation(summary = "마이페이지 답변완료 문의내역", description = "마이페이지 답변완료 문의내역을 조회한다.")
    @GetMapping("/myqna/answer")
    @PreAuthorize("isAuthenticated()")
    public String myqnaAnswer(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                              @Login LoginUser loginUser,
                              Model model) {
        int userId = loginUser.getId();

        int totalRows = qnaService.getTotalQnaRowsByUserId(userId, "Y");

        Pagination pagination = new Pagination(page, totalRows, 5);

        List<Qna> answerQnaList = qnaService.getAnswerQnasByUserId(userId, pagination);

        model.addAttribute("answerQnaList", answerQnaList);
        model.addAttribute("paging", pagination);

        return "user/mypage-qna-answer";
    }

    @Operation(summary = "마이페이지 미답변 문의내역", description = "마이페이지 미답변 문의내역을 조회한다.")
    @GetMapping("/myqna/noAnswer")
    @PreAuthorize("isAuthenticated()")
    public String myqnaNoanswer(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                                @Login LoginUser loginUser,
                                Model model) {
        int userId = loginUser.getId();

        int totalRows = qnaService.getTotalQnaRowsByUserId(userId, "N");

        Pagination pagination = new Pagination(page, totalRows, 5);

        List<Qna> noAnswerQnaList = qnaService.getNoAnswerQnasByUserId(userId, pagination);

        model.addAttribute("noAnswerQnaList", noAnswerQnaList);
        model.addAttribute("paging", pagination);

        return "user/mypage-qna-noanswer";
    }

    @Operation(summary = "문의내역 삭제", description = "문의내역을 삭제한다.")
    @GetMapping("/myqna/delete/{type}/{qnaId}")
    @PreAuthorize("isAuthenticated()")
    public String qnaDelete(@PathVariable("qnaId") int qnaId,
                            @PathVariable("type") String type,
                            @Login LoginUser loginUser,
                            RedirectAttributes redirectAttributes) {
        Qna qna = qnaService.getQnaById(qnaId);
        if (loginUser.getId() != qna.getUser().getId()) {
            throw new RuntimeException("다른 사용자의 문의사항은 삭제할 수 없습니다.");
        }
        qnaService.deleteQna(qnaId);

        redirectAttributes.addFlashAttribute("message", new Message("문의내역이 삭제 되었습니다."));

        return "redirect:/user/myqna/" + type;
    }

    @Operation(summary = "마이페이지 포인트 내역", description = "마이페이지 포인트 내역을 조회한다.")
    @GetMapping("/point")
    @PreAuthorize("isAuthenticated()")
    public String point(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                        @Login LoginUser loginUser,
                        Model model) {

        int userId = loginUser.getId();

        int totalRows = userService.getTotalPointHistory(userId);

        Pagination pagination = new Pagination(page, totalRows, 5);

        List<PointHistoryDto> pointHistoryList = userService.getPointHistoryByUserId(userId, pagination);

        User user = userMapper.getUserById(userId);

        model.addAttribute("balance", user.getPoint());
        model.addAttribute("pointHistoryList", pointHistoryList);
        model.addAttribute("paging", pagination);

        return "user/mypage-point";
    }

    @Operation(summary = "비밀번호 변경 폼", description = "기존회원의 비밀번호 변경 페이지로 이동한다.")
    @GetMapping("/changePwd")
    public String showPasswordChangeForm(Model model) {
        model.addAttribute("passwordChangeForm", new PasswordChangeForm());
        return "user/mypage-resetpassword";
    }

    @Operation(summary = "비밀번호 변경 처리", description = "기존회원의 비밀번호 변경을 처리한다.")
    @PostMapping("/changePwd")
    public String handleChangePassword(@Valid @ModelAttribute PasswordChangeForm form, BindingResult errors, Principal principal) {
        // 현재 로그인한 사용자 이메일 가져오기
        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        String userId = String.valueOf(user.getId());

        // 서버 측 유효성 검사
        if (errors.hasErrors()) {
            return "/user/reset-password";
        }

        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "error.confirmPassword", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            return "user/reset-password";
        }

        // 현재 비밀번호 확인
        if (!userService.checkCurrentPassword(userId, form.getCurrentPassword())) {
            errors.rejectValue("currentPassword", "error.currentPassword", "현재 비밀번호가 일치하지 않습니다.");
            return "user/reset-password";
        }

        // 새 비밀번호 업데이트
        boolean success = userService.updatePassword(userId, form.getNewPassword());
        if (success) {
            return "redirect:/localhost";
        } else {
            // 업데이트 실패 시 처리
            return "redirect:/user/reset-password";
        }
    }

    @Operation(summary = "마이페이지 설정 폼", description = "마이페이지 - 설정 폼을 조회한다.")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/setting")
    public String settingForm(Model model, Principal principal) {
        String email = principal != null ? principal.getName() : null;
        User user = userService.getUserByEmail(email);
        UserSettingForm userSettingForm = new UserSettingForm();

        userSettingForm.setEmail(email);
        userSettingForm.setNickname(user.getNickname());
        userSettingForm.setProfilePhotoUrl(user.getProfilePhoto() != null ? user.getProfilePhoto() : "/images/mypage/default.jpg");

        int userPoint = user.getPoint();
        model.addAttribute("userPoint", userPoint);

        model.addAttribute("userSettingForm", userSettingForm);
        return "user/settingform";
    }

    @Operation(summary = "마이페이지 설정 처리", description = "마이페이지 - 설정에서 기존회원 정보를 수정한다.")
    @PostMapping("/setting")
    public String updateSettings(@ModelAttribute UserSettingForm form,
                                 RedirectAttributes redirectAttributes,
                                 Principal principal) {
        if (principal == null) {
            return "redirect:/user/login"; // 로그인하지 않은 사용자를 로그인 페이지로 리다이렉션
        }
        try {
            // 사용자 설정 업데이트 서비스 호출
            userService.modifyUserSettings(form, principal.getName());

            // 성공 메시지를 리다이렉션 속성에 추가
            redirectAttributes.addFlashAttribute("successMessage", "회원 정보가 성공적으로 업데이트되었습니다.");
            return "redirect:/user/mypage"; // 성공 시 마이페이지 메인
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "업데이트 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/user/setting"; // 실패 시 회원정보 수정 페이지
        }
    }

    @Operation(summary = "인증인가 예외처리", description = "인증,인가 과정에서 문제발생시 오류페이지로 이동한다.")
    @GetMapping("/accessdenied")
    public String accessDenied() {
        return "/user/access-denied";
    }

    @Operation(summary = "이메일 중복 검사", description = "입력된 이메일이 기존에 등록된 이메일인지 검사한다.")
    @PostMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        boolean isExist = userService.isEmailAvailable(email);
        return ResponseEntity.ok(Collections.singletonMap("exists", isExist));
    }

    @Operation(summary = "회원탈퇴", description = "회원탈퇴 처리한다.")
    @PostMapping("/withdraw")
    public String withdrawUser(Principal principal) {
        String email = principal != null ? principal.getName() : null;

        if (email != null) {
            userService.withdrawUser(email);
        }

        return "redirect:/user/logout";
    }

    @Operation(summary = "팔로우 추가", description = "다른 유저를 팔로우한다.")
    @PostMapping("/addFollow")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> createCommunityScrap(@RequestParam("followeeUserId") int followeeUserId,
                                                     @Login LoginUser loginUser) {
        userService.addFollow(loginUser.getId(), followeeUserId);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "팔로우 삭제", description = "커뮤니티글을 스크랩 삭제(취소)한다.")
    @PostMapping("/deleteFollow")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteCommunityScrap(@RequestParam("followeeUserId") int followeeUserId,
                                                     @Login LoginUser loginUser) {
        userService.deleteFollow(loginUser.getId(), followeeUserId);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "리뷰 베스트 순", description = "상품 리뷰를 베스트 순으로 조회한다.")
    @GetMapping("/myReview/best")
    @PreAuthorize("isAuthenticated()")
    public String myreview1(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                            @Login LoginUser loginUser, Model model) {
        int userId = loginUser.getId();

        int totalRows = userService.getReviewRows(userId);
        List<ReviewDto> reviews = userService.getReviewsByBest(userId, page);

        Pagination pagination = new Pagination(page, totalRows, 10);

        model.addAttribute("reviews", reviews);
        model.addAttribute("paging", pagination);
        return "/user/mypage-review-best";
    }

    @Operation(summary = "리뷰 최신순", description = "상품 리뷰를 최신순으로 조회한다.")
    @GetMapping("/myReview/recent")
    @PreAuthorize("isAuthenticated()")
    public String myreview2(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                            @Login LoginUser loginUser, Model model) {
        int userId = loginUser.getId();

        int totalRows = userService.getReviewRows(userId);
        List<ReviewDto> reviews = userService.getReviewsByRecent(userId, page);

        Pagination pagination = new Pagination(page, totalRows, 10);

        model.addAttribute("reviews", reviews);
        model.addAttribute("paging", pagination);
        return "/user/mypage-review-recent";
    }

    @Operation(summary = "리뷰 삭제", description = "상품 리뷰를 삭제한다.")
    @PostMapping("/myReview/delete/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public String deleteReview(@PathVariable int reviewId, @Login LoginUser loginUser, Model model) {
        int userId = loginUser.getId();
        userService.deleteReview(reviewId, userId);
        return "redirect:/user/myReview/best";
    }
}