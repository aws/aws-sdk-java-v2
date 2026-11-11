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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

class PauseResumeHelperTest {
    private final PauseResumeHelper helper = new PauseResumeHelper();
    private File file;

    @BeforeEach
    void setUp() throws IOException {
        file = RandomTempFile.createTempFile("test", UUID.randomUUID().toString());
        Files.write(file.toPath(), RandomStringUtils.randomAlphanumeric(1000).getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        file.delete();
    }

    private static Stream<Arguments> validTokens() {
        return Stream.of(
            Arguments.of("no partSizeInBytes", 1000L, null, 10L, 0L),
            Arguments.of("no totalParts", 1000L, 100L, null, 0L),
            Arguments.of("no transferredParts", 1000L, 100L, 10L, null),
            Arguments.of("part count differs from source file", 2000L, 100L, 20L, 0L),
            Arguments.of("all parts transferred", 1000L, 100L, 10L, 10L),
            Arguments.of("maximum totalParts", 1000L, 1L, 10_000L, 0L));
    }

    private static Stream<Arguments> invalidTokens() {
        return Stream.of(
            Arguments.of("zero partSizeInBytes", 0L, 10L, 0L, "partSizeInBytes"),
            Arguments.of("negative partSizeInBytes", -1L, 10L, 0L, "partSizeInBytes"),
            Arguments.of("zero partSizeInBytes without totalParts", 0L, null, 0L, "partSizeInBytes"),
            Arguments.of("zero totalParts", 100L, 0L, 0L, "totalParts"),
            Arguments.of("negative totalParts", 100L, -1L, 0L, "totalParts"),
            Arguments.of("totalParts above 10000", 100L, 10_001L, 0L, "totalParts"),
            Arguments.of("huge totalParts without partSizeInBytes", null, 2_000_000_000L, 0L, "totalParts"),
            Arguments.of("negative transferredParts", 100L, 10L, -1L, "transferredParts"),
            Arguments.of("transferredParts above totalParts", 100L, 10L, 11L, "transferredParts"),
            Arguments.of("negative transferredParts without totalParts", null, null, -1L, "transferredParts"),
            Arguments.of("transferredParts above 10000 without totalParts", 100L, null, 10_001L, "transferredParts"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validTokens")
    void validateResumeToken_validToken_doesNotThrow(String description, Long fileLength, Long partSizeInBytes,
                                                     Long totalParts, Long transferredParts) {
        ResumableFileUpload upload = upload(fileLength, partSizeInBytes, totalParts, transferredParts);
        assertThatCode(() -> helper.validateResumeToken(upload)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTokens")
    void validateResumeToken_invalidToken_throws(String description, Long partSizeInBytes, Long totalParts,
                                                 Long transferredParts, String invalidField) {
        ResumableFileUpload upload = upload(1000L, partSizeInBytes, totalParts, transferredParts);
        assertThatThrownBy(() -> helper.validateResumeToken(upload))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(invalidField);
    }

    private ResumableFileUpload upload(Long fileLength, Long partSizeInBytes, Long totalParts, Long transferredParts) {
        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                                                               .putObjectRequest(r -> r.bucket("bucket").key("key"))
                                                               .source(file)
                                                               .build();
        return ResumableFileUpload.builder()
                                  .fileLength(fileLength)
                                  .fileLastModified(Instant.ofEpochMilli(file.lastModified()))
                                  .partSizeInBytes(partSizeInBytes)
                                  .totalParts(totalParts)
                                  .transferredParts(transferredParts)
                                  .multipartUploadId("someId")
                                  .uploadFileRequest(uploadFileRequest)
                                  .build();
    }
}
