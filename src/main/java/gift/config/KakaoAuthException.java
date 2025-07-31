package gift.config;

import org.springframework.http.HttpStatusCode;

public class KakaoAuthException extends RuntimeException {


    private final HttpStatusCode httpStatus;

    public KakaoAuthException(String message, Throwable cause, HttpStatusCode httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public HttpStatusCode getHttpStatus() {
        return httpStatus;
    }
}
