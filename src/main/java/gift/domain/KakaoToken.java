package gift.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;

@Entity
public class KakaoToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kakao_token_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "member_id")
    private Member member;

    private String accessToken;
    private String refreshToken;
    private LocalDateTime accessTokenExpiresAt;

    protected KakaoToken() {
    }

    public KakaoToken(Long id, Member member, String accessToken, String refreshToken,
            LocalDateTime accessTokenExpiresAt) {
        this.id = id;
        this.member = member;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public LocalDateTime getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public void renewAccessToken(String accessToken, LocalDateTime accessTokenExpiresAt) {
        renewAccessTokenExpiresAt(accessTokenExpiresAt);
        this.accessToken = accessToken;
    }

    public void renewRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void renewAccessTokenExpiresAt(LocalDateTime accessTokenExpiresAt) {
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.getAccessTokenExpiresAt());
    }
}