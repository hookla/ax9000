package com.ax9k.utils.path;

import com.ax9k.utils.s3.CheckedS3Exception;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class PathLoader {
    private static final Logger LOGGER = LogManager.getLogger();

    private PathLoader() {
        throw new AssertionError("PathLoader is not instantiable");
    }

    public static Path load(String fileLocation) {
        LOGGER.info("Loading file at: {}", fileLocation);

        URL url = normaliseInputPath(fileLocation);

        try {
            return load(url);
        } catch (IOException e) {
            if (e instanceof CheckedS3Exception) {
                throw new RuntimeException("Error downloading input file from S3: " + fileLocation, e.getCause());
            }
            throw new UncheckedIOException("Error copying input file: " + fileLocation, e);
        }
    }

    private static URL normaliseInputPath(String fileLocation) {
        try {
            return Paths.get(fileLocation).toRealPath().toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Given path could not be converted to a URL: " + fileLocation, e);
        } catch (InvalidPathException | NoSuchFileException notLocalPath) {
            try {
                return new URL(fileLocation);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Given input is not a valid local path or URL: " + fileLocation, e);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Error accessing local file: " + fileLocation, e);
        }
    }

    private static Path load(URL url) throws IOException {
        String fileName = extractFileName(url);
        Path localCopy = createTempFile(fileName);
        try (InputStream input = url.openStream();
             OutputStream output = Files.newOutputStream(localCopy, WRITE, TRUNCATE_EXISTING)) {
            input.transferTo(output);
            output.flush();
        }
        LOGGER.info("Created temporary copy at: {}", localCopy);

        return localCopy;
    }

    private static String extractFileName(URL url) {
        String path = url.getPath();
        int nameBeginIndex = path.lastIndexOf('/') + 1;

        return path.substring(nameBeginIndex);
    }

    private static Path createTempFile(String name) throws IOException {
        Path directory = Files.createTempDirectory("ax9k-download");
        Path file = directory.resolve(name);

        try {
            Files.createFile(file);
        } catch (FileAlreadyExistsException ignored) {}

        return file;
    }
}
