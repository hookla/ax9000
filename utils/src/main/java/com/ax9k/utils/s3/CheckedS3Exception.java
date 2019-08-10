package com.ax9k.utils.s3;

import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

public class CheckedS3Exception extends IOException {
    private final S3Exception cause;

    CheckedS3Exception(String message, S3Exception cause) {
        super(message, cause);
        this.cause = cause;
    }

    public S3Exception getCause() {
        return cause;
    }
}
