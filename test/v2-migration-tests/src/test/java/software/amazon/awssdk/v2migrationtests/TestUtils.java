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

package software.amazon.awssdk.v2migrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentest4j.AssertionFailedError;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

public class TestUtils {
    private static final Logger log = Logger.loggerFor(TestUtils.class);

    public static void assertTwoDirectoriesHaveSameStructure(Path a, Path b, Path... ignoredDirectories) {
        Set<Path> ignoredPaths = new HashSet<>(Arrays.asList(ignoredDirectories));
        assertLeftHasRight(a, b, ignoredPaths);
        assertLeftHasRight(b, a, ignoredPaths);
    }

    public static String getVersion() throws IOException {
        Path root = Paths.get(".").normalize().toAbsolutePath();
        Path pomFile = root.resolve("pom.xml");
        Optional<String> versionString =
            Files.readAllLines(pomFile)
                 .stream().filter(l -> l.contains("<version>")).findFirst();

        if (!versionString.isPresent()) {
            throw new AssertionError("No version is found");
        }

        String string = versionString.get().trim();
        String substring = string.substring(9, string.indexOf('/') - 1);
        return substring;
    }

    public static void replaceVersion(Path path, String version) throws IOException {
        try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
            List<String> list = stream.map(line -> line.contains("V2_VERSION") ?
                                                   line.replace("V2_VERSION", version) : line)
                                      .collect(Collectors.toList());
            Files.write(path, list, StandardCharsets.UTF_8);
        }
    }

    private static void assertLeftHasRight(Path left, Path right, Set<Path> ignoredDirectories) {
        if (ignoredDirectories.contains(left)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(left)) {
            paths.filter(leftPath -> !ignoredDirectories.contains(leftPath))
                 .forEach(leftPath -> {
                     Path leftRelative = left.relativize(leftPath);
                     Path rightPath = right.resolve(leftRelative);
                     log.debug(() -> String.format("Comparing %s with %s", leftPath, rightPath));
                     try {
                         assertThat(rightPath).exists();
                     } catch (AssertionError e) {
                         throw new AssertionFailedError(e.getMessage(), toFileTreeString(left), toFileTreeString(right));
                     }
                     if (Files.isRegularFile(leftPath)) {
                         assertThat(leftPath).hasSameBinaryContentAs(rightPath);
                     }
                 });
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to compare %s with %s", left, right), e);
        }
    }

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

    public static Result run(Path dir, String... args) {
        String applicationHome = System.getProperty("application.home");

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        // Map<String, String> environment = processBuilder.environment();
        // environment.put("HOME", applicationHome);
        processBuilder.redirectErrorStream(true);
        Result result = new Result();

        if (dir != null) {
            processBuilder.directory(dir.toFile());
        }

        String location = dir == null ? "" : "(From: " + dir + ")";
        log.info(() -> "> Executing Command: " + location + " " + Arrays.toString(args));

        try {
            Process process = processBuilder.start();
            try {

                IoUtils.copy(process.getInputStream(), System.out);
                result.result = process.waitFor();
                if (!result.wasSuccessful()) {
                    throw new RuntimeException("Command (" + Arrays.toString(args) + ") failed: " + result.output);
                }
            } finally {
                process.destroy();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public static class Result {
        private String output;
        private int result;

        public String output() {
            return output;
        }

        public boolean wasSuccessful() {
            return result == 0;
        }
    }
}
