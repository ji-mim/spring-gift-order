package gift.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gift.dto.Jwk;
import gift.dto.JwkSet;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
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
            throw new UnAuthorizationException("유효하지 않은 id token 입니다.");
        }
        String[] parts = idToken.split("\\.");
        String payload = parts[1];
        JsonNode root = decodeBase64Json(payload);

        return root.has("email") ? root.get("email").asText() : null;
    }

    public boolean validateIdToken(String idToken) {
        String[] parts = idToken.split("\\.");
        String header = parts[0];
        String payload = parts[1];

        return validatePayload(payload) && validateSignature(header, payload, parts);
    }

    private static boolean validateSignature(String header, String payload, String[] parts)  {
        JsonNode headerJson = decodeBase64Json(header);
        String kidByIdToken = headerJson.has("kid") ? headerJson.get("kid").asText() : null;
        if (kidByIdToken == null) {
            throw new UnAuthorizationException("ID 토큰에 kid 값이 없습니다.");
        }
        RestClient client = RestClient.builder().baseUrl("https://kauth.kakao.com/.well-known/jwks.json").build();
        JwkSet jwkSet = client.get().retrieve()
                .body(new ParameterizedTypeReference<JwkSet>() {
                });

        Jwk matchingJwk = jwkSet.keys().stream().filter(jwk -> jwk.kid().equals(kidByIdToken))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("공개키를 찾을 수 없습니다."));
        PublicKey publicKey = buildRSAPublicKey(matchingJwk.n(), matchingJwk.e());

        return verifySignature(header + "." + payload, Base64.getUrlDecoder().decode(parts[2]), publicKey);
    }

    private boolean validatePayload(String payload) {
        JsonNode jsonNode = decodeBase64Json(payload);
        String iss = jsonNode.has("iss") ? jsonNode.get("iss").asText() : null;
        String aud = jsonNode.has("aud") ? jsonNode.get("aud").asText() : null;
        Long exp = jsonNode.has("exp") ? jsonNode.get("exp").asLong() : null;

        if (iss == null || aud == null || exp == null) {
            throw new UnAuthorizationException("idToken이 유효하지 않습니다.");
        }
        return iss.equals("https://kauth.kakao.com") && aud.equals(API_KEY) && Instant.now().isBefore(Instant.ofEpochSecond(exp));
    }

    public static boolean verifySignature(String headerPayload, byte[] signatureBytes, PublicKey publicKey) {
        Signature signature = null;
        try {
            signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(headerPayload.getBytes(StandardCharsets.UTF_8));
            return signature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException("서명 검증 실패", e);
        }
    }

    private static JsonNode decodeBase64Json(String jwt)  {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(jwt);
            String decodedJson = new String(decodedBytes);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(decodedJson);
            return root;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JWt 추출 실패", e);
        }
    }

    public static PublicKey buildRSAPublicKey(String nBase64Url, String eBase64Url) {
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(nBase64Url));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(eBase64Url));
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("RSA 공개키 생성 실패",e);
        }
    }
}
