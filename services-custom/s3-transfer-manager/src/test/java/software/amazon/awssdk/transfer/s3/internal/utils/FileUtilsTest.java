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

package software.amazon.awssdk.transfer.s3.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileUtilsTest {

    private static final byte[] CONTENT = "content".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path tempDir;

    @BeforeEach
    void assumePosix() {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
    }

    @Test
    void writeOwnerOnly_newFile_createsOwnerOnlyFile() throws IOException {
        Path file = tempDir.resolve("token.json");

        FileUtils.writeOwnerOnly(file, CONTENT);

        assertOwnerOnlyWithContent(file);
    }

    @Test
    void writeOwnerOnly_existingFile_restrictsToOwnerAndOverwrites() throws IOException {
        Path file = tempDir.resolve("token.json");
        Files.write(file, "old content that is longer".getBytes(StandardCharsets.UTF_8));
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));

        FileUtils.writeOwnerOnly(file, CONTENT);

        assertOwnerOnlyWithContent(file);
    }

    @Test
    void writeOwnerOnly_symlinkToExistingFile_restrictsTargetAndKeepsLink() throws IOException {
        Path target = tempDir.resolve("target.json");
        Files.write(target, "old".getBytes(StandardCharsets.UTF_8));
        Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-r--r--"));
        Path link = Files.createSymbolicLink(tempDir.resolve("token.json"), target);

        FileUtils.writeOwnerOnly(link, CONTENT);

        assertThat(Files.isSymbolicLink(link)).isTrue();
        assertOwnerOnlyWithContent(target);
    }

    @Test
    void writeOwnerOnly_danglingSymlink_createsOwnerOnlyTarget() throws IOException {
        Path target = tempDir.resolve("target.json");
        Path link = Files.createSymbolicLink(tempDir.resolve("token.json"), target);

        FileUtils.writeOwnerOnly(link, CONTENT);

        assertThat(Files.isSymbolicLink(link)).isTrue();
        assertOwnerOnlyWithContent(target);
    }

    @Test
    void writeOwnerOnly_danglingRelativeSymlink_createsOwnerOnlyTargetNextToLink() throws IOException {
        Path link = Files.createSymbolicLink(tempDir.resolve("token.json"), Paths.get("target.json"));

        FileUtils.writeOwnerOnly(link, CONTENT);

        assertOwnerOnlyWithContent(tempDir.resolve("target.json"));
    }

    @Test
    void writeOwnerOnly_danglingSymlinkChain_createsOwnerOnlyFinalTarget() throws IOException {
        Path target = tempDir.resolve("target.json");
        Path middle = Files.createSymbolicLink(tempDir.resolve("middle.json"), target);
        Path link = Files.createSymbolicLink(tempDir.resolve("token.json"), middle);

        FileUtils.writeOwnerOnly(link, CONTENT);

        assertOwnerOnlyWithContent(target);
    }

    @Test
    void writeOwnerOnly_existingDirectory_throwsIOException() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("token.json"));

        assertThatThrownBy(() -> FileUtils.writeOwnerOnly(dir, CONTENT)).isInstanceOf(IOException.class);
    }

    private static void assertOwnerOnlyWithContent(Path file) throws IOException {
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(file))).isEqualTo("rw-------");
        assertThat(Files.readAllBytes(file)).isEqualTo(CONTENT);
    }
}
