package com.biyesheji.onlinehomework.security;

import com.biyesheji.onlinehomework.model.User;
import com.biyesheji.onlinehomework.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * 自定义用户详情服务
 * 实现了 Spring Security 的 UserDetailsService 接口，用于加载用户认证信息
 * 该服务在用户登录认证和 JWT token 验证时都会被调用
 *
 * @author毕业设计
 * @version1.0.0
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * 用户数据库操作接口，用于查询用户信息
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * 根据用户名加载用户详情
     * 这是 UserDetailsService 接口的核心方法，Spring Security 在认证时会调用此方法
     *
     * @param username 用户名
     * @return UserDetails 对象，包含用户的认证信息和权限
     * @throws UsernameNotFoundException 如果找不到指定用户名的用户
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库中查找用户
        User user = userRepository.findByUsername(username)
                // 如果找不到用户，抛出 UsernameNotFoundException 异常
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在：" + username));

        // 将用户信息转换为 Spring Security 的 UserDetails 对象
        // 参数说明：
        // - user.getUsername()：用户名
        // - user.getPassword()：加密后的密码
        // - 权限列表：将用户角色转换为 Spring Security 格式（ROLE_角色名）
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            // 将用户角色包装为 Spring Security 的权限对象
            // 角色名称会转换为大写，并添加 "ROLE_" 前缀
            // 例如：STUDENT -> ROLE_STUDENT，TEACHER -> ROLE_TEACHER
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()))
        );
    }
}