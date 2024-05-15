package com.roommake.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordForm {
    private String email;

    @NotBlank(message = "새 비밀번호는 필수 입력값입니다.")
    @Size(min = 8, max = 16, message = "비밀번호는 최소 8글자 이상, 16자 이하만 가능합니다.")
    @Pattern(
            regexp = "(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,16}",
            message = "비밀번호는 8~16자 영문 대 소문자, 숫자, 특수문자를 사용하세요."
    )
    private String newPassword;

    @NotBlank(message = "비밀번호 확인은 필수 입력값입니다.")
    private String confirmPassword;
}
