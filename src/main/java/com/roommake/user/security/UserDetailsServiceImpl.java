package com.roommake.user.security;

import com.roommake.user.enums.UserStatusEnum;
import com.roommake.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 이메일로 사용자 및 권한 정보를 조회
        Map<String, Object> userMap = userMapper.getUserByEmailWithRoles(email);
        if (userMap == null) {
            throw new UsernameNotFoundException("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + email);
        }
        int id = (int) userMap.get("user_id");
        String nickname = (String) userMap.get("user_nickname");
        String fetchedEmail = (String) userMap.get("user_email");
        String password = (String) userMap.get("user_password");
        String rolesString = (String) userMap.get("roles");
        String userStatus = (String) userMap.get("user_status");

        UserStatusEnum statusEnum = UserStatusEnum.valueOf(userStatus.toUpperCase());
        if (statusEnum == UserStatusEnum.BLOCK || statusEnum == UserStatusEnum.DELETE) {
            throw new UsernameNotFoundException("사용자 상태로 인해 로그인할 수 없습니다.");
        }

        String[] rolesArray = rolesString.split(",");
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : rolesArray) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.trim()));
        }

        // UserDetails 객체 생성하여 반환

        return new UserDetailsImpl(id, nickname, fetchedEmail, password, authorities);
    }
}
