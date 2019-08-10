package com.ax9k.utils.s3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class S3Uploader {
    private static final Logger LOGGER = LogManager.getLogger();

    private final PutObjectRequest.Builder partialRequest;
    private final Region region;

    public S3Uploader(String bucket) {
        requireNonNull(bucket, "S3Uploader: bucket can not be null");

        String[] location = bucket.split("/");
        if (location.length > 1) {
            bucket = location[1];
            region = Region.of(location[0]);
        } else {
            bucket = location[0];
            region = null;
        }

        partialRequest = PutObjectRequest.builder().bucket(bucket);
    }

    public void upload(Path file, Path s3key) {
        partialRequest.key(withOnlyForwardSlashes(s3key));
        PutObjectRequest request = partialRequest.build();
        try (S3Client client = createClient()) {
            client.putObject(request, RequestBody.of(file));
            LOGGER.info("Uploaded file to S3. Bucket: {}, Key: {}", request.bucket(), request.key());
        }
    }

    private String withOnlyForwardSlashes(Path path) {
        return path.toString().replace('\\', '/');
    }

    private S3Client createClient() {
        S3Client client;
        if (region == null) {
            client = S3Client.create();
        } else {
            client = S3Client.builder().region(region).build();
        }
        return client;
    }
}
