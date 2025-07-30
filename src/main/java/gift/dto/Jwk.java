package gift.dto;

public record Jwk(
        String kid,
        String kty,
        String alg,
        String use,
        String n,
        String e
) {
}
