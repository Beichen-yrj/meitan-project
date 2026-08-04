package com.meitan.config;

import com.meitan.entity.User;
import com.meitan.mapper.UserMapper;
import com.meitan.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    writeSecurityError(response, 401, "登录状态已失效，请重新登录"))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                    writeSecurityError(response, 403, "没有权限访问该接口"))
            )
            .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writeSecurityError(HttpServletResponse response,
                                           int status,
                                           String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        String escapedMessage = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write(
            "{\"code\":" + status + ",\"message\":\"" + escapedMessage + "\",\"data\":null}"
        );
    }

    @Bean
    public OncePerRequestFilter jwtAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                String token = extractToken(request);
                if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
                    Long userId = jwtUtils.getUserId(token);
                    User user = userMapper.selectById(userId);
                    if (user != null
                            && Integer.valueOf(1).equals(user.getStatus())
                            && !Integer.valueOf(1).equals(user.getBlacklisted())) {
                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                userId, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                            );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        request.setAttribute("userId", userId);
                    }
                }
                chain.doFilter(request, response);
            }

            private String extractToken(HttpServletRequest request) {
                String bearer = request.getHeader("Authorization");
                if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
                    return bearer.substring(7);
                }
                return null;
            }
        };
    }
}
