package com.roommake.user.controller;

import com.roommake.user.exception.EmailException;
import com.roommake.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Email API", description = "이메일 인증을 제공한다.")
@RestController
@RequiredArgsConstructor
public class EmailVerifyController {

    private final UserService userService;

    @Operation(summary = "인증코드 발송", description = "이메일로 인증 코드를 발송하는 엔드포인트")
    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendVerifyCode(@RequestParam String email) {
        try {
            userService.sendVerifyCode(email);
            return ResponseEntity.ok("입력한 이메일로 인증 메일이 발송되었습니다.\n이메일에 표시된 인증코드를 입력해주세요.");
        } catch (EmailException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("이메일 발송 중 오류가 발생했습니다.");
        }
    }

    @Operation(summary = "인증코드 검증", description = "제공된 인증코드를 검증한다.")
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String code, @RequestParam String email, HttpSession session) {
        boolean isValidCode = userService.verifyEmail(email, code);
        if (isValidCode) {
            session.setAttribute("verifiedEmail", email); // 세션에 이메일 저장
            // 인증 코드가 유효한 경우
            return ResponseEntity.ok().body("{\"valid\": true}");
        } else {
            // 인증 코드가 유효하지 않은 경우
            return ResponseEntity.badRequest().body("{\"valid\": false}");
        }
    }

    @Operation(summary = "이메일 서비스 예외 처리", description = "이메일 서비스에서 발생하는 모든 예외를 처리한다.")
    @ExceptionHandler(EmailException.class)
    public ResponseEntity<String> handleEmailServiceException(EmailException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류로 인증 코드 발송에 실패하였습니다.");
    }
}
