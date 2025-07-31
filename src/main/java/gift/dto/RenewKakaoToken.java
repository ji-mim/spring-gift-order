package gift.dto;

public record RenewKakaoToken(
        String access_token,
        int expires_in,
        String refresh_token
) {
}
