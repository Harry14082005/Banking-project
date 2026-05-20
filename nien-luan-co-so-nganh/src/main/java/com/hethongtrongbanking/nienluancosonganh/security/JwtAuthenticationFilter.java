package com.hethongtrongbanking.nienluancosonganh.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    // Ham nay tu dong chay moi khi bat ki ai goi API (CustomerUserDetailsService)

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Xet header de lay chuoi JWT
            String jwt = getJwtFromRequest(request);

            // Bo vao JwtTokenProvider xem Token co ton tai khong
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Hop le -> Doc ten ra
                String username = tokenProvider.getUsernameFromJWT(jwt);

                // Upload thong tin day du cua user tu DB
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                // Danh dau "Da kiem duyet" (Create Authentication Object)
                UsernamePasswordAuthenticationToken authenticaion = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                // Luu thong tin chi tiet vao Context
                authenticaion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Luu thong tin vao Security ContextHolder
                SecurityContextHolder.getContext().setAuthentication(authenticaion);
            }

        } catch (Exception ex) {
            log.error("Khong the cai dat quyen truy cap cho user nay", ex);

        }
        // Neu khong co loi thi cho di tiep
        filterChain.doFilter(request, response);
    }

    // Tach chuoi Token ra khoi chu "Bearer"
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        // Luat quoc te: Token luon bat dau voi "Bearer"
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // cat 7 ky tu dau di
        }
        return null;

    }
    // Ex: headerName: Authorization; headerValue: Bearer abc123xyz.jwt.token.123
    // Return: abc123xyz.jwt.token.123

}
