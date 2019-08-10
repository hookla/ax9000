package com.ax9k.utils.s3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.core.sync.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

public class S3Downloader {
    private static final Logger LOGGER = LogManager.getLogger();

    private final GetObjectRequest request;
    private final Path path;
    private final Region region;

    public S3Downloader(Path path, String bucket) {
        this.path = requireNonNull(path, "S3Downloader path");
        requireNonNull(bucket, "S3Downloader bucket");

        String[] location = bucket.split("/");
        if (location.length > 1) {
            bucket = location[1];
            region = Region.of(location[0]);
        } else {
            bucket = location[0];
            region = null;
        }

        request = GetObjectRequest.builder()
                                  .bucket(bucket)
                                  .key(withOnlyForwardSlashes(path))
                                  .build();
    }

    private String withOnlyForwardSlashes(Path path) {
        return path.toString().replace('\\', '/');
    }

    public S3Downloader(Path path, String bucket, String regionName) {
        this.path = path;
        this.region = Region.of(regionName);

        request = GetObjectRequest.builder()
                                  .bucket(bucket)
                                  .key(withOnlyForwardSlashes(path))
                                  .build();
    }

    public Path download() throws IOException {
        Path destination = createTemporaryFile();

        downloadTo(destination);

        return destination;
    }

    private Path createTemporaryFile() throws IOException {
        String name = path.getFileName().toString();
        Path file = Paths.get(System.getProperty("java.io.tmpdir"), name);

        Files.deleteIfExists(file);
        Files.createFile(file);
        file.toFile().deleteOnExit();

        return file.toRealPath();
    }

    private void downloadTo(Path destination) throws S3Exception, IOException {
        S3Client client;
        if (region == null) {
            client = S3Client.create();
        } else {
            client = S3Client.builder().region(region).build();
        }

        try (client;
             OutputStream destinationStream = Files.newOutputStream(destination)) {
            LOGGER.info("Downloading {}", path);
            ResponseInputStream<GetObjectResponse> response = client.getObject(request);
            GetObjectResponse responseHeader = response.response();
            LOGGER.info("Content-Type: {}", responseHeader.contentType());

            response.transferTo(destinationStream);
            destinationStream.flush();
        }
    }
}