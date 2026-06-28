package com.example.security;

import com.example.util.JwtUtil;
import com.example.util.TokenRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenRedisService tokenRedisService;

    @Value("${jwt.header}")
    private String tokenHeader;

    @Value("${jwt.prefix}")
    private String tokenPrefix;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(tokenHeader);

        if (authHeader != null && authHeader.startsWith(tokenPrefix)) {
            String token = authHeader.substring(tokenPrefix.length()).trim();
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token);
            Long studentId = jwtUtil.getStudentIdFromToken(token);

            if (username != null && userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (!jwtUtil.isTokenExpired(token) && tokenRedisService.validateToken(userId, token)) {
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());
                    LoginUser loginUser = new LoginUser(userId, username, role, studentId);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            loginUser, null, Collections.singletonList(authority));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        chain.doFilter(request, response);
    }
}
