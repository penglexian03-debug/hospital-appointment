package com.example.hospital.common.util;

import com.example.hospital.common.context.LoginUser;
import com.example.hospital.common.exception.BusinessException;
import com.example.hospital.common.result.ResultCode;
import com.example.hospital.config.JwtProperties;
import com.example.hospital.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + properties.getExpirationMs());
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("realName", user.getRealName())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public LoginUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);
            String realName = claims.get("realName", String.class);
            Number roleValue = claims.get("role", Number.class);
            Integer role = roleValue == null ? null : roleValue.intValue();
            return new LoginUser(userId, username, realName, role);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态无效或已过期");
        }
    }

    public long getExpirationSeconds() {
        return properties.getExpirationMs() / 1000;
    }
}
