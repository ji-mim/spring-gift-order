package gift.dto;

import java.time.LocalDateTime;

public record OrdersResponse(
        Long id,
        Long optionId,
        int quantity,
        LocalDateTime now,
        String message) {
}
