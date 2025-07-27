package gift.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final String secretKey = "SecretKey12345678901234567890123456789012123145423123411441";

    private final long expirationMs = 1000 * 60 * 60; // 1시간

    public String createToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String authHeader) {
        String token = authHeader.substring(7);
        try {
            Jwts.parser()
                    .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String extractNickname(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            String payload = parts[1];

            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String decodedJson = new String(decodedBytes);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(decodedJson);

            return root.has("nickname") ? root.get("nickname").asText() : null;

        } catch (Exception e) {
            throw new RuntimeException("JWT nickname 추출 실패", e);
        }
    }
}
