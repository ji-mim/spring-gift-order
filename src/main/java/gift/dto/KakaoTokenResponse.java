package gift.dto;

public record KakaoTokenResponse(
        String accessToken,
        String refresh_token,
        int expires_in
) {
}
