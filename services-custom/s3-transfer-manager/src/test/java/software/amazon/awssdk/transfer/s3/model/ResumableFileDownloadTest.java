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

package software.amazon.awssdk.transfer.s3.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static software.amazon.awssdk.utils.DateUtils.parseIso8601Date;

import com.google.common.jimfs.Jimfs;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Arrays;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.testutils.RandomTempFile;

class ResumableFileDownloadTest {

    private static final Instant DATE1 = parseIso8601Date("2022-05-13T21:55:52.529Z");
    private static final Instant DATE2 = parseIso8601Date("2022-05-15T21:50:11.308Z");

    private static FileSystem jimfs;
    private static ResumableFileDownload standardDownloadObject;

    @BeforeAll
    public static void setup() {
        jimfs = Jimfs.newFileSystem();
        standardDownloadObject = resumableFileDownload();
    }

    @AfterAll
    public static void tearDown() {
        try {
            jimfs.close();
        } catch (IOException e) {
            // no-op
        }
    }

    @Test
    void equalsHashcode() {
        EqualsVerifier.forClass(ResumableFileDownload.class)
                      .withNonnullFields("downloadFileRequest")
                      .verify();
    }

    @Test
    void fileSerDeser() throws IOException {
        String directoryName = "test";
        Path directory = jimfs.getPath(directoryName);
        Files.createDirectory(directory);

        Path file = jimfs.getPath(directoryName, "serializedDownload");
        standardDownloadObject.serializeToFile(file);

        ResumableFileDownload deserializedDownload = ResumableFileDownload.fromFile(file);
        assertThat(deserializedDownload).isEqualTo(standardDownloadObject);
    }

    @Test
    void stringSerDeser() {
        String serializedDownload = standardDownloadObject.serializeToString();
        ResumableFileDownload deserializedDownload = ResumableFileDownload.fromString(serializedDownload);
        assertThat(deserializedDownload).isEqualTo(standardDownloadObject);
    }

    @Test
    void bytesSerDeser()  {
        SdkBytes serializedDownload = standardDownloadObject.serializeToBytes();
        ResumableFileDownload deserializedDownload =
            ResumableFileDownload.fromBytes(SdkBytes.fromByteArrayUnsafe(serializedDownload.asByteArray()));
        assertThat(deserializedDownload).isEqualTo(standardDownloadObject);
    }

    @Test
    void inputStreamSerDeser() throws IOException {
        InputStream serializedDownload = standardDownloadObject.serializeToInputStream();
        ResumableFileDownload deserializedDownload =
            ResumableFileDownload.fromBytes(SdkBytes.fromInputStream(serializedDownload));
        assertThat(deserializedDownload).isEqualTo(standardDownloadObject);
    }

    @Test
    void outputStreamSer() throws IOException {
        ByteArrayOutputStream serializedDownload = new ByteArrayOutputStream();
        standardDownloadObject.serializeToOutputStream(serializedDownload);
        ResumableFileDownload deserializedDownload =
            ResumableFileDownload.fromBytes(SdkBytes.fromByteArrayUnsafe(serializedDownload.toByteArray()));
        assertThat(deserializedDownload).isEqualTo(standardDownloadObject);
    }

    @Test
    void byteBufferDeser()  {
        SdkBytes serializedDownload = standardDownloadObject.serializeToBytes();
        ResumableFileDownload deserializedDownload =
            ResumableFileDownload.fromBytes(SdkBytes.fromByteBuffer(serializedDownload.asByteBuffer()));
        assertThat(deserializedDownload).isEqualTo(standardDownloadObject);
    }

    @Test
    void serializeToFile_newFile_isOwnerOnly(@TempDir Path tempDir) throws IOException {
        assumePosix();
        Path file = tempDir.resolve("token.json");

        standardDownloadObject.serializeToFile(file);

        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(file))).isEqualTo("rw-------");
        assertThat(ResumableFileDownload.fromFile(file)).isEqualTo(standardDownloadObject);
    }

    @Test
    void serializeToFile_existingFile_isRestrictedToOwnerAndOverwritten(@TempDir Path tempDir) throws IOException {
        assumePosix();
        Path file = tempDir.resolve("token.json");
        Files.write(file, "old content that is longer than nothing".getBytes(StandardCharsets.UTF_8));
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));

        standardDownloadObject.serializeToFile(file);

        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(file))).isEqualTo("rw-------");
        assertThat(new String(Files.readAllBytes(file), StandardCharsets.UTF_8))
            .isEqualTo(standardDownloadObject.serializeToString());
    }

    @Test
    void serializeToFile_missingParentDirectory_throwsSdkClientException(@TempDir Path tempDir) {
        assumePosix();
        Path file = tempDir.resolve("missing").resolve("token.json");

        assertThatThrownBy(() -> standardDownloadObject.serializeToFile(file))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Failed to write to");
    }

    private static void assumePosix() {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
    }

    private static ResumableFileDownload resumableFileDownload() {
        Path path = RandomTempFile.randomUncreatedFile().toPath();

        return ResumableFileDownload.builder()
                                    .downloadFileRequest(r -> r.getObjectRequest(b -> b.bucket("BUCKET")
                                                                                 .key("KEY")
                                                                                 .partNumber(1)
                                                                                 .ifModifiedSince(DATE1))
                                                               .destination(path))
                                    .bytesTransferred(1000L)
                                    .fileLastModified(DATE1)
                                    .s3ObjectLastModified(DATE2)
                                    .totalSizeInBytes(2000L)
                                    .completedParts(Arrays.asList(1, 2, 5))
                                    .build();
    }
}
