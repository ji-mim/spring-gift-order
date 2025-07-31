package gift.dto;

public record KakaoTokenResponse(
        String access_token,
        String refresh_token,
        String id_token,
        int expires_in
) {
}
