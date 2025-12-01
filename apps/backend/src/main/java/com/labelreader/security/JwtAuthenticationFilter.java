package com.labelreader.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            System.out.println("DEBUG: Validating token for: " + request.getRequestURI());
            boolean isValid = jwtUtil.isTokenValid(token);
            boolean isExpired = jwtUtil.isTokenExpired(token);
            System.out.println("DEBUG: Token valid: " + isValid + ", expired: " + isExpired);

            if (isValid && !isExpired) {
                Long userId = jwtUtil.extractUserId(token);
                String email = jwtUtil.extractEmail(token);
                String userType = jwtUtil.extractUserType(token);
                System.out.println("DEBUG: userId=" + userId + ", email=" + email + ", userType=" + userType);

                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userType);
                System.out.println("DEBUG: Creating authority: " + authority.getAuthority());

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(authority));

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("DEBUG: Authentication set successfully with authorities: " + authentication.getAuthorities());
                System.out.println("DEBUG: Is authenticated: " + authentication.isAuthenticated());
            } else {
                System.out.println("DEBUG: Token validation failed or expired");
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Exception during token validation: " + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }
}
