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
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;
import static software.amazon.awssdk.utils.DateUtils.parseIso8601Date;

import com.google.common.jimfs.Jimfs;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.testutils.RandomTempFile;

class ResumableFileUploadTest {

    private static final Instant DATE = parseIso8601Date("2022-05-15T21:50:11.308Z");

    private static FileSystem jimfs;
    private static ResumableFileUpload resumeableFileUpload;

    @BeforeAll
    public static void setup() {
        jimfs = Jimfs.newFileSystem();
        resumeableFileUpload = ResumableFileUpload();
    }

    @Test
    void equalsHashcode() {
        EqualsVerifier.forClass(ResumableFileUpload.class)
                      .withNonnullFields("fileLength", "uploadFileRequest", "fileLastModified")
                      .verify();
    }

    @Test
    void toBuilder() {
        ResumableFileUpload fileUpload =
            ResumableFileUpload.builder()
                               .multipartUploadId("1234")
                               .uploadFileRequest(UploadFileRequest.builder().putObjectRequest(p -> p.bucket("bucket").key("key"
                               )).source(Paths.get("test")).build())
                               .fileLastModified(Instant.now())
                               .fileLength(10L)
                               .partSizeInBytes(10 * MB)
                               .build();

        assertThat(fileUpload.toBuilder().build()).isEqualTo(fileUpload);
        assertThat(fileUpload.toBuilder().multipartUploadId("5678").build()).isNotEqualTo(fileUpload);
    }

    @Test
    void fileSerDeser() throws IOException {
        String directoryName = "test";
        Path directory = jimfs.getPath(directoryName);
        Files.createDirectory(directory);

        Path file = jimfs.getPath(directoryName, "serializedDownload");
        resumeableFileUpload.serializeToFile(file);

        ResumableFileUpload deserializedDownload = ResumableFileUpload.fromFile(file);
        assertThat(deserializedDownload).isEqualTo(resumeableFileUpload);
    }

    @Test
    void stringSerDeser() {
        String serializedDownload = resumeableFileUpload.serializeToString();
        ResumableFileUpload deserializedDownload = ResumableFileUpload.fromString(serializedDownload);
        assertThat(deserializedDownload).isEqualTo(resumeableFileUpload);
    }

    @Test
    void bytesSerDeser() {
        SdkBytes serializedDownload = resumeableFileUpload.serializeToBytes();
        ResumableFileUpload deserializedDownload =
            ResumableFileUpload.fromBytes(SdkBytes.fromByteArrayUnsafe(serializedDownload.asByteArray()));
        assertThat(deserializedDownload).isEqualTo(resumeableFileUpload);
    }

    @Test
    void inputStreamSerDeser() {
        InputStream serializedDownload = resumeableFileUpload.serializeToInputStream();
        ResumableFileUpload deserializedDownload =
            ResumableFileUpload.fromBytes(SdkBytes.fromInputStream(serializedDownload));
        assertThat(deserializedDownload).isEqualTo(resumeableFileUpload);
    }

    @Test
    void outputStreamSer() {
        ByteArrayOutputStream serializedDownload = new ByteArrayOutputStream();
        resumeableFileUpload.serializeToOutputStream(serializedDownload);
        ResumableFileUpload deserializedDownload =
            ResumableFileUpload.fromBytes(SdkBytes.fromByteArrayUnsafe(serializedDownload.toByteArray()));
        assertThat(deserializedDownload).isEqualTo(resumeableFileUpload);
    }

    @Test
    void byteBufferDeser() {
        SdkBytes serializedDownload = resumeableFileUpload.serializeToBytes();
        ResumableFileUpload deserializedDownload =
            ResumableFileUpload.fromBytes(SdkBytes.fromByteBuffer(serializedDownload.asByteBuffer()));
        assertThat(deserializedDownload).isEqualTo(resumeableFileUpload);
    }

    private static ResumableFileUpload ResumableFileUpload() {
        Path path = RandomTempFile.randomUncreatedFile().toPath();
        Map<String, String> metadata = new HashMap<>();

        return ResumableFileUpload.builder()
                                  .uploadFileRequest(r -> r.putObjectRequest(b -> b.bucket("BUCKET")
                                                                                   .key("KEY")
                                                                                   .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                                                                   .bucketKeyEnabled(Boolean.FALSE)
                                                                                   .metadata(metadata))
                                                           .source(path))
                                  .fileLength(5000L)
                                  .fileLastModified(DATE)
                                  .partSizeInBytes(1024L)
                                  .totalParts(5L)
                                  .build();
    }

}
