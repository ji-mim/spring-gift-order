package gift.dto;

public record OrdersRequest(
        Long optionId,
        int quantity,
        String message
) {

}
