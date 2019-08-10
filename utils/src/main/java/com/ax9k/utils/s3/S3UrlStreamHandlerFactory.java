package com.ax9k.utils.s3;

import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;

public class S3UrlStreamHandlerFactory extends URLStreamHandlerProvider {
    private static final String AMAZON_S3_PROTOCOL = "s3";

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (AMAZON_S3_PROTOCOL.equalsIgnoreCase(protocol)) {
            return new S3UrlStreamHandler();
        }
        return null;
    }
}
