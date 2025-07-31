package gift.dto;

import java.util.List;

public record JwkSet(
        List<Jwk> keys
) {

}
