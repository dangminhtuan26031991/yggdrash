package io.yggdrash.core.exception;

public class FileNewAccountException extends RuntimeException {
    public FileNewAccountException() {
        super();
    }

    public FileNewAccountException(String s) {
        super(s);
    }

    public FileNewAccountException(Throwable cause) {
        super(cause);
    }
}
