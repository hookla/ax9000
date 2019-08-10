package com.ax9k.utils.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.regions.Region;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class S3UrlConnectionTest {
    private String bucket;
    private String path;
    private String region;
    private URL s3Url;

    private static URL createUrl(String bucket, String path, String region) {
        String urlText = "s3://" + bucket + "/" + path;
        if (region != null) {
            urlText += "?region=" + region;
        }
        return createUrl(urlText);
    }

    private static URL createUrl(String urlText) {
        try {
            return new URL(null, urlText, new S3UrlStreamHandler());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private S3UrlConnection getConnection(URL url) {
        try {
            return (S3UrlConnection) url.openConnection();
        } catch (IOException wontHappen) {
            throw new AssertionError(wontHappen);
        }
    }

    @Nested
    class WhenGivenValidBucketAndKey {
        @BeforeEach
        void initialiseTestEnvironment() {
            bucket = "example";
            path = "path/to/file";
            region = null;
            s3Url = createUrl(bucket, path, region);
        }

        @Test
        void shouldCorrectlyIdentifyMissingRegion() {
            S3UrlConnection s3Connection = getConnection(s3Url);
            assertEquals(Optional.empty(), s3Connection.getRegion());
        }

        @Test
        void shouldCorrectlyIdentifyBucketName() {
            S3UrlConnection s3Connection = getConnection(s3Url);
            assertEquals(bucket, s3Connection.getBucket());
        }

        @Test
        void shouldCorrecltyIdentifyPath() {
            S3UrlConnection s3Connection = getConnection(s3Url);
            assertEquals(path, s3Connection.getPath());
        }
    }

    @Nested
    class WhenGivenValidBucketKeyAndRegion {
        @BeforeEach
        void initialiseTestEnvironment() {
            bucket = "example";
            path = "path/to/file";
            region = "ap-southeast-1";
            s3Url = createUrl(bucket, path, region);
        }

        @Test
        void shouldCorrectlyIdentifyRegion() {
            S3UrlConnection s3Connection = getConnection(s3Url);
            assertEquals(Region.AP_SOUTHEAST_1, s3Connection.getRegion().orElseThrow());
        }
    }

    @Nested
    class WhenGivenInvalidBucket {
        @BeforeEach
        void initialiseTestEnvironment() {
            bucket = "";
            path = "path/to/file";
            region = null;
            s3Url = createUrl(bucket, path, region);
        }

        @Test
        void shouldFailOnEmptyBucket() {
            assertThrows(IllegalArgumentException.class, () -> getConnection(s3Url));
        }
    }

    @Nested
    class WhenGivenInvalidPath {
        @BeforeEach
        void initialiseTestEnvironment() {
            bucket = "example";
            path = "";
            region = null;
            s3Url = createUrl(bucket, path, region);
        }

        @Test
        void shouldFailOnEmptyBucket() {
            assertThrows(IllegalArgumentException.class, () -> getConnection(s3Url));
        }
    }

    @Nested
    class WhenGivenInvalidRegion {
        @BeforeEach
        void initialiseTestEnvironment() {
            bucket = "example";
            path = "path/to/file";
            region = "";
            s3Url = createUrl(bucket, path, region);
        }

        @Test
        void shouldRevertToDefaultRegion() {
            S3UrlConnection s3Connection = getConnection(s3Url);
            assertEquals(Optional.empty(), s3Connection.getRegion());
        }
    }
}