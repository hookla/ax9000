package com.ax9k.utils.s3;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

class S3UrlStreamHandler extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(URL u) {
        return new S3UrlConnection(u);
    }
}
