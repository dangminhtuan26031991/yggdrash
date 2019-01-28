package io.yggdrash.core.exception;

public class FailInjectException extends RuntimeException {
    public FailInjectException() {
        super();
    }

    public FailInjectException(String s) {
        super(s);
    }

    public FailInjectException(Throwable cause) {
        super(cause);
    }
}
