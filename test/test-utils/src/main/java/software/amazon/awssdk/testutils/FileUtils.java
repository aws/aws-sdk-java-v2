/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.testutils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;
import software.amazon.awssdk.utils.StringUtils;

public final class FileUtils {
    private FileUtils() {

    }

    public static void cleanUpTestDirectory(Path directory) {
        if (directory == null) {
            return;
        }

        try {
            try (Stream<Path> paths = Files.walk(directory, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)) {
                paths.sorted(Comparator.reverseOrder())
                     .map(Path::toFile)
                     .forEach(File::delete);
            }

        } catch (IOException e) {
            // ignore
            e.printStackTrace();
        }
    }

    public static void copyDirectory(Path source, Path destination, CopyOption... options) {
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Files.createDirectories(mirror(dir));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, mirror(file), options);
                    return FileVisitResult.CONTINUE;
                }

                private Path mirror(Path path) {
                    Path relativePath = source.relativize(path);
                    return destination.resolve(relativePath);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to copy %s to %s", source, destination), e);
        }
    }

    /**
     * Convert a given directory into a visual file tree. E.g., given an input structure of:
     * <pre>
     *     /tmp/testdir
     *     /tmp/testdir/CHANGELOG.md
     *     /tmp/testdir/README.md
     *     /tmp/testdir/notes
     *     /tmp/testdir/notes/2022
     *     /tmp/testdir/notes/2022/1.txt
     *     /tmp/testdir/notes/important.txt
     *     /tmp/testdir/notes/2021
     *     /tmp/testdir/notes/2021/2.txt
     *     /tmp/testdir/notes/2021/1.txt
     * </pre>
     * Calling this method on {@code /tmp/testdir} will yield the following output:
     * <pre>
     *     - testdir
     *         - CHANGELOG.md
     *         - README.md
     *         - notes
     *             - 2022
     *                 - 1.txt
     *             - important.txt
     *             - 2021
     *                 - 2.txt
     *                 - 1.txt
     * </pre>
     */
    public static String toFileTreeString(Path root) {
        int rootDepth = root.getNameCount();
        String tab = StringUtils.repeat(" ", 4);
        StringBuilder sb = new StringBuilder();
        try (Stream<Path> files = Files.walk(root)) {
            files.forEach(p -> {
                int indentLevel = p.getNameCount() - rootDepth;
                String line = String.format("%s- %s", StringUtils.repeat(tab, indentLevel), p.getFileName());
                sb.append(line);
                sb.append(System.lineSeparator());
            });
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to convert %s to file tree", root), e);
        }
        return sb.toString();
    }
}
