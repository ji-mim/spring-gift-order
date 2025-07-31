package gift.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gift.dto.Jwk;
import gift.dto.JwkSet;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class JwtTokenProvider {

    private final String secretKey = "SecretKey12345678901234567890123456789012123145423123411441";

    private final long expirationMs = 1000 * 60 * 60; // 1시간

    @Value("${kakao.api.key}")
    private String API_KEY;

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

    public String extractEmail(String idToken) {
        if (!validateIdToken(idToken)) {
            throw new UnAuthorizationException("유효하지 않은 ID 토큰입니다.");
        }
        Claims claims = Jwts.parser()
                .setSigningKey(extractPublicKeyFromIdToken(idToken))
                .build()
                .parseClaimsJws(idToken)
                .getBody();

        return claims.get("email", String.class);
    }

    public boolean validateIdToken(String idToken) {
        try {
            PublicKey publicKey = extractPublicKeyFromIdToken(idToken);
            Claims claims = Jwts.parser()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(idToken)
                .getBody();

            return "https://kauth.kakao.com".equals(claims.getIssuer()) &&
                claims.getAudience().contains(API_KEY);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private PublicKey extractPublicKeyFromIdToken(String idToken) {
        String[] parts = idToken.split("\\.");
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String kid;
        try {
            kid = new ObjectMapper().readTree(headerJson).get("kid").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json 직렬화에 실패했습니다.");
        }
        RestClient client = RestClient.builder()
                .baseUrl("https://kauth.kakao.com/.well-known/jwks.json")
                .build();

        JwkSet jwkSet = client.get().retrieve().body(new ParameterizedTypeReference<JwkSet>() {});
        Jwk jwk = jwkSet.keys().stream()
                .filter(k -> String.valueOf(k.kid()).equals(kid))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("일치하는 공개키가 없습니다."));

        return buildRSAPublicKey(jwk.n(), jwk.e());
    }


    public static PublicKey buildRSAPublicKey(String nBase64Url, String eBase64Url) {
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(nBase64Url));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(eBase64Url));
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("RSA 공개키 생성 실패",e);
        }
    }
}
