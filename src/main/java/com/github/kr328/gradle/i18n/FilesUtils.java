package com.github.kr328.gradle.i18n;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FilesUtils {
    public static void deleteChildren(final Path path) throws IOException {
        try (final DirectoryStream<Path> files = Files.newDirectoryStream(path)) {
            for (final Path file : files) {
                if (Files.isDirectory(file)) {
                    deleteChildren(file);
                }
                Files.delete(file);
            }
        }
    }
}
