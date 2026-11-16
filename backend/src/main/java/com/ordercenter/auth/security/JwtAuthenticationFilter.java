package com.ordercenter.auth.security;

import com.ordercenter.auth.AuthPrincipal;
import com.ordercenter.auth.JwtService;
import com.ordercenter.auth.JwtTokenException;
import com.ordercenter.auth.entity.AppUser;
import com.ordercenter.auth.entity.AuthSession;
import com.ordercenter.auth.repository.AppUserRepository;
import com.ordercenter.auth.repository.AuthSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 解析 Bearer access token，并检查对应会话是否仍然有效。
 * 因为每次都查 auth_sessions，退出登录后旧 access token 会立刻被拒绝。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AuthSessionRepository sessionRepository;
    private final AppUserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AuthSessionRepository sessionRepository,
            AppUserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null
                && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                authenticate(token, request);
            } catch (JwtTokenException exception) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        JwtService.AccessTokenClaims claims = jwtService.parse(token);
        LocalDateTime now = LocalDateTime.now();

        AuthSession session = sessionRepository.findById(claims.sessionId())
                .orElse(null);
        if (session == null || !session.isActive(now)) {
            return;
        }

        AppUser user = userRepository.findById(claims.userId()).orElse(null);
        if (user == null || !user.isEnabled()) {
            return;
        }

        AuthPrincipal principal = new AuthPrincipal(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                session.getId()
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                );
        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
