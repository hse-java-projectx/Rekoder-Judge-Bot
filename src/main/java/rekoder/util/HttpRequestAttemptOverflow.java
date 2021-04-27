package rekoder.util;

public class HttpRequestAttemptOverflow extends Exception {
    public HttpRequestAttemptOverflow(String message) {
        super(message);
    }

    public HttpRequestAttemptOverflow(Throwable e) {
        super(e);
    }
}
