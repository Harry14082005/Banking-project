package com.hethongtrongbanking.nienluancosonganh.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component // Class bo tro, tien ich chung
@Slf4j
public class JwtTokenProvider {

    // Lay chuoi ma khoa bi mat tu application.properties
    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private Long jwtExpirationMs;

    // Tao JWT tu thong tin user sau khi login thanh cong
    public String generateToken(Authentication authentication) {
        // Ly do dung UserDetails thay User
        // -> no khong quan User/ Customer / Admin gi. Chi quan tam chuan UserDetails de
        // goi getUsername()
        // trong truong hop nay van co the xai User
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        // Bam khoa
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername()) // luu username vao token
                .setIssuedAt(new Date()) // thoi gian tao
                .setExpiration(expiryDate) // thoi gian het han
                .signWith(key, SignatureAlgorithm.HS512) // key ten bang thuat toan HS512
                .compact(); // bien thanh chuoi jwt
    }

    // Quyet the: Lay Username nguoc ra tu JWT
    public String getUsernameFromJWT(String token) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key) // dua key vao
                .build()
                .parseClaimsJws(token) // so khop chu ky
                .getBody(); // lay phan noi body bo qua Header + Signature
        return claims.getSubject();
    }

    // Kiem tra token hop le hay khong
    public boolean validateToken(String authToken) {

        try {
            Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parserBuilder() // Spring tao builder lap rap may quet Token
                    .setSigningKey(key) // dua "chia khoa bi mat" vao may
                    .build() // Dong nap may lai
                    .parseClaimsJws(authToken); // bo token vao quet
            return true;

        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false; // Neu 1 trong cac loi tren xay ra
    }

}
