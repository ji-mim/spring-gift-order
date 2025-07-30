package gift.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record OrdersRequest(
        @NotBlank(message = "optionId 값은 필수입니다.")
        Long optionId,
        @Positive(message = "주문 수량은 0보다 커야합니다.")
        @NotBlank(message = "주문 수량은 필수입니다.")
        int quantity,
        String message
) {

}
