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

package software.amazon.awssdk.transfer.s3.internal.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.utils.DateUtils.parseIso8601Date;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.RequestPayer;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

class ResumableFileUploadSerializerTest {

    private static final Instant DATE = parseIso8601Date("2022-05-15T21:50:11.308Z");

    private static final Path PATH = RandomTempFile.randomUncreatedFile().toPath();
    private static final Map<String, PutObjectRequest> PUT_OBJECT_REQUESTS;

    static {
        Map<String, PutObjectRequest> requests = new HashMap<>();
        requests.put("EMPTY", PutObjectRequest.builder().build());
        requests.put("STANDARD", PutObjectRequest.builder().bucket("BUCKET").key("KEY").build());
        Map<String, String> metadata = new HashMap<>();
        metadata.put("foo", "bar");
        requests.put("ALL_TYPES", PutObjectRequest.builder()
                                                  .bucket("BUCKET")
                                                  .key("KEY")
                                                  .acl(ObjectCannedACL.PRIVATE)
                                                  .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                                  .requestPayer(RequestPayer.REQUESTER)
                                                  .metadata(metadata)
                                                  .build());
        PUT_OBJECT_REQUESTS = Collections.unmodifiableMap(requests);
    }

    @ParameterizedTest
    @MethodSource("uploadObjects")
    void serializeDeserialize_ShouldWorkForAllUploads(ResumableFileUpload upload) {
        byte[] serializedUpload = ResumableFileUploadSerializer.toJson(upload);
        ResumableFileUpload deserializedUpload = ResumableFileUploadSerializer.fromJson(serializedUpload);

        assertThat(deserializedUpload).isEqualTo(upload);
    }

    @Test
    void serializeDeserialize_fromStoredString_ShouldWork() {
        ResumableFileUpload upload =
            ResumableFileUpload.builder()
                               .uploadFileRequest(d -> d.source(Paths.get("test/request"))
                                                        .putObjectRequest(PUT_OBJECT_REQUESTS.get("ALL_TYPES")))
                               .fileLength(5000L)
                               .fileLastModified(parseIso8601Date("2022-03-08T10:15:30Z"))
                               .multipartUploadId("id")
                               .totalParts(40L)
                               .partSizeInBytes(1024L)
                               .build();

        byte[] serializedUpload = ResumableFileUploadSerializer.toJson(upload);
        assertThat(new String(serializedUpload, StandardCharsets.UTF_8)).isEqualTo(SERIALIZED_UPLOAD_OBJECT);

        ResumableFileUpload deserializedUpload =
            ResumableFileUploadSerializer.fromJson(SERIALIZED_UPLOAD_OBJECT.getBytes(StandardCharsets.UTF_8));
        assertThat(deserializedUpload).isEqualTo(upload);
    }

    @Test
    void serializeDeserialize_DoesNotPersistConfiguration() {
        ResumableFileUpload upload =
            ResumableFileUpload.builder()
                               .uploadFileRequest(d -> d.source(PATH)
                                                        .putObjectRequest(PUT_OBJECT_REQUESTS.get("STANDARD"))
                                                        .addTransferListener(LoggingTransferListener.create()))
                               .fileLength(5000L)
                               .fileLastModified(parseIso8601Date("2022-03-08T10:15:30Z"))
                               .build();

        byte[] serializedUpload = ResumableFileUploadSerializer.toJson(upload);
        ResumableFileUpload deserializedUpload = ResumableFileUploadSerializer.fromJson(serializedUpload);

        UploadFileRequest fileRequestWithoutConfig =
            upload.uploadFileRequest().copy(r -> r.transferListeners((List) null));
        assertThat(deserializedUpload).isEqualTo(upload.copy(d -> d.uploadFileRequest(fileRequestWithoutConfig)));
    }

    @Test
    void serializeDeserialize_DoesNotPersistRequestOverrideConfiguration() {
        PutObjectRequest requestWithOverride =
            PutObjectRequest.builder()
                            .bucket("BUCKET")
                            .key("KEY")
                            .overrideConfiguration(c -> c.apiCallAttemptTimeout(Duration.ofMillis(20)).build())
                            .build();

        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                                                               .source(PATH)
                                                               .putObjectRequest(requestWithOverride)
                                                               .build();

        ResumableFileUpload upload = ResumableFileUpload.builder()
                                                        .uploadFileRequest(uploadFileRequest)
                                                        .fileLastModified(DATE)
                                                        .fileLength(1000L)
                                                        .build();

        byte[] serializedUpload = ResumableFileUploadSerializer.toJson(upload);
        ResumableFileUpload deserializedUpload = ResumableFileUploadSerializer.fromJson(serializedUpload);

        PutObjectRequest requestWithoutOverride =
            requestWithOverride.copy(r -> r.overrideConfiguration((AwsRequestOverrideConfiguration) null));
        UploadFileRequest fileRequestCopy = uploadFileRequest.copy(r -> r.putObjectRequest(requestWithoutOverride));
        assertThat(deserializedUpload).isEqualTo(upload.copy(d -> d.uploadFileRequest(fileRequestCopy)));
    }

    public static Collection<ResumableFileUpload> uploadObjects() {
        return Stream.of(differentUploadSettings(),
                         differentPutObjects())
                     .flatMap(Collection::stream).collect(Collectors.toList());
    }

    private static List<ResumableFileUpload> differentPutObjects() {
        return PUT_OBJECT_REQUESTS.values()
                                  .stream()
                                  .map(request -> resumableFileUpload(1000L, null, null, null))
                                  .collect(Collectors.toList());
    }

    private static List<ResumableFileUpload> differentUploadSettings() {

        return Arrays.asList(
            resumableFileUpload(null, null, null, null),
            resumableFileUpload(1000L, null, null, null),
            resumableFileUpload(1000L, 5L, 1L, null),
            resumableFileUpload(1000L, 5L, 2L, "1234")
        );
    }

    private static ResumableFileUpload resumableFileUpload(Long partSizeInBytes,
                                                           Long totalNumberOfParts,
                                                           Long transferredParts,
                                                           String multipartUploadId) {
        UploadFileRequest request = downloadRequest(PATH, PUT_OBJECT_REQUESTS.get("STANDARD"));
        return ResumableFileUpload.builder()
                                  .uploadFileRequest(request)
                                  .fileLength(1000L)
                                  .multipartUploadId(multipartUploadId)
                                  .fileLastModified(DATE)
                                  .partSizeInBytes(partSizeInBytes)
                                  .totalParts(totalNumberOfParts)
                                  .transferredParts(transferredParts)
                                  .build();
    }

    private static UploadFileRequest downloadRequest(Path path, PutObjectRequest request) {
        return UploadFileRequest.builder()
                                .putObjectRequest(request)
                                .source(path)
                                .build();
    }

    private static final String SERIALIZED_UPLOAD_OBJECT = "{\"fileLength\":5000,\"fileLastModified\":1646734530.000,"
                                                           + "\"multipartUploadId\":\"id\",\"partSizeInBytes\":1024,"
                                                           + "\"totalParts\":40,\"uploadFileRequest\":{\"source\":\"test"
                                                           + "/request\",\"putObjectRequest\":{\"x-amz-acl\":\"private\","
                                                           + "\"Bucket\":\"BUCKET\",\"x-amz-sdk-checksum-algorithm\":\"CRC32\","
                                                           + "\"Key\":\"KEY\",\"x-amz-meta-\":{\"foo\":\"bar\"},"
                                                           + "\"x-amz-request-payer\":\"requester\"}}}";
}
