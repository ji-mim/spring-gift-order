package gift.repository;

import gift.domain.KakaoToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KakaoTokenJpaRepository extends JpaRepository<KakaoToken, Long> {

    Optional<KakaoToken> findByMemberId(Long memberId);
}
