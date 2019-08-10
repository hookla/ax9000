package com.ax9k.utils.s3;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.core.sync.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class S3UrlConnection extends URLConnection {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int ALLOW_EMPTY_TRAILING_STRINGS = -1;
    private static final String QUERY_SEPARATOR = "&";
    private static final String KEY_VALUE_SEPARATOR = "=";
    private static final int UNKNOWN = -1;

    private final String bucket, path;
    private final Optional<Region> region;

    private S3Client client;
    private GetObjectResponse responseHeaders;
    private ResponseInputStream<GetObjectResponse> response;

    S3UrlConnection(URL url) {
        super(notNull(url));

        bucket = notBlank(url.getHost(), "S3 URL needs a bucket name: %s", url);
        path = notBlank(removeStart(url.getPath(), "/"),
                        "S3 URL needs a path to the input file within the bucket: %s", url);
        region = extractRegion(url);
    }

    private Optional<Region> extractRegion(URL url) {
        String query = url.getQuery();
        if (StringUtils.isBlank(query)) {
            return Optional.empty();
        }

        Map<String, String> parameters = Stream.of(query)
                                               .map(q -> q.split(QUERY_SEPARATOR, ALLOW_EMPTY_TRAILING_STRINGS))
                                               .flatMap(Stream::of)
                                               .map(q -> q.split(KEY_VALUE_SEPARATOR, ALLOW_EMPTY_TRAILING_STRINGS))
                                               .collect(Collectors.toMap(kv -> kv.length == 0 ? "?" : kv[0],
                                                                         kv -> kv.length < 2 ? "" : kv[1]));

        return Optional.ofNullable(parameters.get("region"))
                       .filter(StringUtils::isNotBlank)
                       .map(String::toLowerCase)
                       .map(Region::of);
    }

    public String getBucket() {
        return bucket;
    }

    public String getPath() {
        return path;
    }

    public Optional<Region> getRegion() {
        return region;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        return new S3ClientClosingInputStream(client, response);
    }

    @Override
    public void connect() throws IOException {
        S3ClientBuilder partialClient = S3Client.builder();
        region.ifPresent(partialClient::region);

        LOGGER.info("Opening connection to S3 object. Bucket: {}, Key: {}, Region: {}",
                    bucket, path, region.map(Region::toString).orElse("DEFAULT"));

        try {
            client = partialClient.build();
            GetObjectRequest request = GetObjectRequest.builder()
                                                       .bucket(bucket)
                                                       .key(path)
                                                       .build();

            response = client.getObject(request);
            responseHeaders = response.response();
        } catch (S3Exception e) {
            throw new CheckedS3Exception("Error downloading object from S3", e);
        }

        LOGGER.info("Content-Type: {}, Size: {} KiB",
                    getContentType(), getContentLengthLong() > UNKNOWN ?
                                      toKibibytes(getContentLengthLong()) :
                                      "UNKNOWN");
    }

    private static double toKibibytes(long bytes) {
        return bytes / 1024d;
    }

    @Override
    public long getContentLengthLong() {
        return responseHeaders.contentLength();
    }

    @Override
    public String getContentType() {
        return responseHeaders.contentType();
    }

    @Override
    public int getContentLength() {
        long actualLength = getContentLengthLong();
        return actualLength <= Integer.MAX_VALUE ? (int) actualLength : -1;
    }

    @Override
    public String getContentEncoding() {
        return responseHeaders.contentEncoding();
    }

    @Override
    public long getExpiration() {
        return responseHeaders.expires().toEpochMilli();
    }

    @Override
    public long getLastModified() {
        return responseHeaders.lastModified().toEpochMilli();
    }
}
