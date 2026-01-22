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

package software.amazon.awssdk.services.s3.s3express;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumsEnabledValidator.CHECKSUM;
import static software.amazon.awssdk.testutils.service.AwsTestBase.CREDENTIALS_PROVIDER_CHAIN;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.rules.testing.AsyncTestCase;
import software.amazon.awssdk.core.rules.testing.SyncTestCase;
import software.amazon.awssdk.core.rules.testing.model.Expect;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.internal.plugins.S3OverrideAuthSchemePropertiesPlugin;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumType;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListDirectoryBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.utils.IoUtils;

/**
 * S3Express integration tests, covering all supported APIs
 */
public class S3ExpressIntegrationTest extends S3ExpressIntegrationTestBase {
    private static final String KEY = "b/small.txt";
    private static final String CONTENTS = "test";
    private static final String COPY_DESTINATION_KEY = "copy/smallcopied.txt";
    private static final Region TEST_REGION = Region.US_EAST_1;
    private static final CapturingInterceptor capturingInterceptor = new CapturingInterceptor();
    private static final String AZ = "use1-az4";
    private static S3Client s3;
    private static S3AsyncClient s3Async;
    private static S3AsyncClient s3CrtAsync;
    private static String testBucket;

    private static final String S3EXPRESS_BUCKET_PATTERN = temporaryBucketName(S3ExpressIntegrationTest.class) +"--%s--x-s3";

    private static String getS3ExpressBucketNameForAz(String az) {
        return String.format(S3EXPRESS_BUCKET_PATTERN, az);
    }

    @BeforeAll
    static void setup() {
        s3 = s3ClientBuilder(TEST_REGION).overrideConfiguration(o -> o.addExecutionInterceptor(capturingInterceptor))
                                         .build();
        s3Async = s3AsyncClientBuilder(TEST_REGION).overrideConfiguration(o -> o.addExecutionInterceptor(capturingInterceptor))
                                                   .build();
        s3CrtAsync = s3CrtAsyncClientBuilder(TEST_REGION).build();
        testBucket = getS3ExpressBucketNameForAz(AZ);
        createBucketS3Express(s3, testBucket, AZ);
    }

    @AfterAll
    static void teardown() {
        deleteBucketAndAllContents(s3, testBucket);
        s3.close();
        s3Async.close();
        s3CrtAsync.close();
    }

    @BeforeEach
    void reset() {
        capturingInterceptor.reset();
    }

    private static Stream<S3AsyncClient> asyncClients() {
        return Stream.of(s3Async, s3CrtAsync);
    }

    @ParameterizedTest(autoCloseArguments = false)
    @MethodSource("asyncClients")
    public void putCopyGetDeleteAsync(S3AsyncClient s3AsyncClient) {
        s3AsyncClient.putObject(r -> r.bucket(testBucket).key(KEY), AsyncRequestBody.fromString(CONTENTS)).join();
        s3AsyncClient.headObject(r -> r.bucket(testBucket).key(KEY)).join();

        s3.copyObject(r -> r.sourceBucket(testBucket).sourceKey(KEY).destinationBucket(testBucket).destinationKey(COPY_DESTINATION_KEY));
        s3AsyncClient.headObject(r -> r.bucket(testBucket).key(COPY_DESTINATION_KEY)).join();

        String result = s3AsyncClient.getObject(r -> r.bucket(testBucket).key(KEY), AsyncResponseTransformer.toBytes()).join().asUtf8String();
        assertThat(result).isEqualTo(CONTENTS);

        s3AsyncClient.deleteObject(r -> r.bucket(testBucket).key(KEY)).join();
    }

    @Test
    public void putCopyGetDeleteSync() {
        s3.putObject(r -> r.bucket(testBucket).key(KEY), RequestBody.fromString(CONTENTS));
        s3.headObject(r -> r.bucket(testBucket).key(KEY));

        s3.copyObject(r -> r.sourceBucket(testBucket).sourceKey(KEY).destinationBucket(testBucket).destinationKey(COPY_DESTINATION_KEY));
        s3.headObject(r -> r.bucket(testBucket).key(COPY_DESTINATION_KEY));

        String result = s3.getObject(r -> r.bucket(testBucket).key(KEY), ResponseTransformer.toBytes()).asUtf8String();
        assertThat(result).isEqualTo(CONTENTS);

        s3.deleteObject(r -> r.bucket(testBucket).key(KEY));
    }

    @ParameterizedTest(autoCloseArguments = false)
    @MethodSource("asyncClients")
    public void uploadMultiplePartAsync(S3AsyncClient s3AsyncClient) {
        String uploadId = s3AsyncClient.createMultipartUpload(b -> b.bucket(testBucket).key(KEY)).join().uploadId();

        UploadPartRequest uploadPartRequest = UploadPartRequest.builder().bucket(testBucket).key(KEY)
                                                               .uploadId(uploadId)
                                                               .partNumber(1)
                                                               .build();

        UploadPartResponse response = s3AsyncClient.uploadPart(uploadPartRequest, AsyncRequestBody.fromString(CONTENTS)).join();

        List<CompletedPart> completedParts = new ArrayList<>();
        completedParts.add(CompletedPart.builder().eTag(response.eTag()).partNumber(1).build());
        CompletedMultipartUpload completedUploadParts = CompletedMultipartUpload.builder().parts(completedParts).build();
        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                                                                                       .bucket(testBucket)
                                                                                       .key(KEY)
                                                                                       .uploadId(uploadId)
                                                                                       .multipartUpload(completedUploadParts)
                                                                                       .build();
        CompleteMultipartUploadResponse completeMultipartUploadResponse = s3AsyncClient.completeMultipartUpload(completeRequest).join();
        assertThat(completeMultipartUploadResponse).isNotNull();

        ResponseBytes<GetObjectResponse> objectAsBytes = s3.getObject(b -> b.bucket(testBucket).key(KEY), ResponseTransformer.toBytes());
        String appendedString = String.join("", CONTENTS);
        assertThat(objectAsBytes.asUtf8String()).isEqualTo(appendedString);
    }

    @ParameterizedTest(autoCloseArguments = false)
    @MethodSource("asyncClients")
    public void uploadMultiplePartAsync_withChecksum(S3AsyncClient s3AsyncClient) {
        String uploadId = s3AsyncClient.createMultipartUpload(b -> b.bucket(testBucket)
                                                                    .checksumAlgorithm(ChecksumAlgorithm.CRC64_NVME)
                                                                    .checksumType(ChecksumType.FULL_OBJECT)
                                                                    .key(KEY)).join().uploadId();


        UploadPartRequest uploadPartRequest = UploadPartRequest.builder().bucket(testBucket).key(KEY)
                                                               .uploadId(uploadId)
                                                               .checksumAlgorithm(ChecksumAlgorithm.CRC64_NVME)
                                                               .partNumber(1)
                                                               .build();

        UploadPartResponse response = s3AsyncClient.uploadPart(uploadPartRequest, AsyncRequestBody.fromString(CONTENTS)).join();

        List<CompletedPart> completedParts = new ArrayList<>();
        completedParts.add(CompletedPart.builder()
                                        .checksumCRC64NVME(response.checksumCRC64NVME())
                                        .eTag(response.eTag()).partNumber(1).build());
        CompletedMultipartUpload completedUploadParts = CompletedMultipartUpload.builder().parts(completedParts).build();
        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                                                                                       .bucket(testBucket)
                                                                                       .key(KEY)
                                                                                       .checksumType(ChecksumType.FULL_OBJECT)
                                                                                       .uploadId(uploadId)
                                                                                       .multipartUpload(completedUploadParts)
                                                                                       .build();
        CompleteMultipartUploadResponse completeMultipartUploadResponse = s3AsyncClient.completeMultipartUpload(completeRequest).join();
        assertThat(completeMultipartUploadResponse).isNotNull();

        ResponseBytes<GetObjectResponse> objectAsBytes = s3.getObject(b -> b.bucket(testBucket).key(KEY), ResponseTransformer.toBytes());
        String appendedString = String.join("", CONTENTS);
        assertThat(objectAsBytes.asUtf8String()).isEqualTo(appendedString);
    }

    @MethodSource("syncTestCases")
    @ParameterizedTest
    public void s3Express_nonObjectTransferApis_Sync(SyncTestCase tc) {
        runAndVerify(tc);
    }

    @MethodSource("asyncTestCases")
    @ParameterizedTest(autoCloseArguments = false)
    public void s3Express_nonObjectTransferApis_Async(AsyncTestCase tc) {
        runAndVerify(tc);
    }

    @Test
    public void putObject_withUserCalculatedChecksum_doesNotAddMultipleHeadersOrPerformMd5Validation() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] data = CONTENTS.getBytes(StandardCharsets.UTF_8);

        md.update(data);
        byte[] checksum = md.digest();
        String checksumVal = Base64.getEncoder().encodeToString(checksum);

        PutObjectRequest request = PutObjectRequest.builder()
                                                   .bucket(testBucket)
                                                   .key(KEY)
                                                   .checksumSHA1(checksumVal)
                                                   .build();

        s3.putObject(request, RequestBody.fromString(CONTENTS));
        assertThat(capturingInterceptor.capturedRequests()).hasSize(1);
        assertThat(capturingInterceptor.isMd5Enabled).isFalse();
    }

    @Test
    public void putObject_payloadSigningEnabledSra_executesSuccessfully() {
        S3Client s3Client = S3Client.builder()
                                    .region(TEST_REGION)
                                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                    .addPlugin(S3OverrideAuthSchemePropertiesPlugin.enablePayloadSigningPlugin())
                                    .build();

        PutObjectRequest request = PutObjectRequest.builder()
                                                   .bucket(testBucket)
                                                   .key(KEY)
                                                   .build();

        PutObjectResponse response = s3Client.putObject(request, RequestBody.fromString(CONTENTS));
        assertThat(response.versionId()).isNull();
    }

    @Test
    public void s3Presigner_s3ExpressConfigurationUseS3Express_presignsS3ExpressRequestSuccessfully() {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                            .key(KEY)
                                                            .bucket(testBucket)
                                                            .build();
        s3.putObject(putObjectRequest, RequestBody.fromBytes("content".getBytes(StandardCharsets.UTF_8)));

        S3Presigner presigner = S3Presigner.builder()
                                           .region(TEST_REGION)
                                           .s3Client(s3)
                                           .build();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(testBucket).key(KEY).build();
        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                                                                                 .getObjectRequest(getObjectRequest)
                                                                                 .signatureDuration(Duration.ofMinutes(11))
                                                                                 .build();

        PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);
        try (InputStream response = presignedGetObjectRequest.url().openConnection().getInputStream()) {
            assertThat(IoUtils.toUtf8String(response)).isEqualTo("content");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void s3Presigner_s3ExpressConfigurationS3ClientConfigured_presignsS3ExpressRequestSuccessfully() {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                            .key(KEY)
                                                            .bucket(testBucket)
                                                            .build();
        s3.putObject(putObjectRequest, RequestBody.fromBytes("content".getBytes(StandardCharsets.UTF_8)));

        S3Presigner presigner = S3Presigner.builder()
                                           .region(TEST_REGION)
                                           .s3Client(s3)
                                           .build();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(testBucket).key(KEY).build();
        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                                                                                 .getObjectRequest(getObjectRequest)
                                                                                 .signatureDuration(Duration.ofMinutes(11))
                                                                                 .build();

        PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);
        try (InputStream response = presignedGetObjectRequest.url().openConnection().getInputStream()) {
            assertThat(IoUtils.toUtf8String(response)).isEqualTo("content");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void s3Presigner_withS3ExpressAuthDisabled_presignsS3ExpressRequestSuccessfully() {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                            .key(KEY)
                                                            .bucket(testBucket)
                                                            .build();
        s3.putObject(putObjectRequest, RequestBody.fromBytes("content".getBytes(StandardCharsets.UTF_8)));

        S3Presigner presigner = S3Presigner.builder()
                                           .region(TEST_REGION)
                                           .build();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(testBucket).key(KEY).build();
        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                                                                                 .getObjectRequest(getObjectRequest)
                                                                                 .signatureDuration(Duration.ofMinutes(11))
                                                                                 .build();

        PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);
        try (InputStream response = presignedGetObjectRequest.url().openConnection().getInputStream()) {
            assertThat(IoUtils.toUtf8String(response)).isEqualTo("content");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<SyncTestCase> syncTestCases() {
        Endpoint controlEndpoint = Endpoint.builder().url(URI.create("https://s3express-control.us-east-1.amazonaws.com/")).build();
        return Arrays.asList(
            //control plane APIs
            new SyncTestCase("ListDirectoryBuckets", () -> {
                ListDirectoryBucketsRequest request = ListDirectoryBucketsRequest.builder().build();
                s3.listDirectoryBuckets(request);
            }, Expect.builder().endpoint(controlEndpoint).build()),
            new SyncTestCase("PutBucketPolicy", () -> {
                PutBucketPolicyRequest request = PutBucketPolicyRequest.builder().bucket(testBucket).policy("fake").build();
                s3.putBucketPolicy(request);
            }, Expect.builder().endpoint(controlEndpoint).error("Policies must be valid JSON").build()),
            new SyncTestCase("GetBucketPolicy", () -> {
                GetBucketPolicyRequest request = GetBucketPolicyRequest.builder().bucket(testBucket).build();
                s3.getBucketPolicy(request);
            }, Expect.builder().endpoint(controlEndpoint).error("The bucket policy does not exist").build()),
            new SyncTestCase("DeleteBucketPolicy", () -> {
                DeleteBucketPolicyRequest request = DeleteBucketPolicyRequest.builder().bucket(testBucket).build();
                s3.deleteBucketPolicy(request);
            }, Expect.builder().endpoint(controlEndpoint).build()),
            //data plane APIs
            new SyncTestCase("ListObjectsV2", () -> {
                ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(testBucket).build();
                s3.listObjectsV2(request);
            }, Expect.builder().build()),
            new SyncTestCase("DeleteObjects", () -> {
                DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                                                                   .bucket(testBucket)
                                                                   .delete(Delete.builder()
                                                                                 .objects(ObjectIdentifier.builder()
                                                                                                          .key("fake")
                                                                                                          .build())
                                                                                 .build())
                                                                   .build();
                s3.deleteObjects(request);
            }, Expect.builder().build()),
            new SyncTestCase("HeadBucket", () -> {
                HeadBucketRequest request = HeadBucketRequest.builder().bucket(testBucket).build();
                s3.headBucket(request);
            }, Expect.builder().build())
        );
    }

    private static List<AsyncTestCase> asyncTestCases() {
        return Stream.concat(asyncTestCasesPerClient(s3Async).stream(), asyncTestCasesPerClient(s3CrtAsync).stream()).collect(Collectors.toList());
    }

    private static List<AsyncTestCase> asyncTestCasesPerClient(S3AsyncClient s3Async) {
        // getSimpleName is not "simple", but it's fine to be used for testing
        String simpleName = s3Async.getClass().getSimpleName();
        return Arrays.asList(
            //control plane APIs
            new AsyncTestCase("ListDirectoryBuckets-" + simpleName, () -> {
                ListDirectoryBucketsRequest request = ListDirectoryBucketsRequest.builder().build();
                return s3Async.listDirectoryBuckets(request);
            }, Expect.builder().build()),
            new AsyncTestCase("PutBucketPolicy-" + simpleName, () -> {
                PutBucketPolicyRequest request = PutBucketPolicyRequest.builder().bucket(testBucket).policy("fake").build();
                return s3Async.putBucketPolicy(request);
            }, Expect.builder().error("Policies must be valid JSON").build()),
            new AsyncTestCase("GetBucketPolicy-" + simpleName, () -> {
                GetBucketPolicyRequest request = GetBucketPolicyRequest.builder().bucket(testBucket).build();
                return s3Async.getBucketPolicy(request);
            }, Expect.builder().error("The bucket policy does not exist").build()),
            new AsyncTestCase("DeleteBucketPolicy-" + simpleName, () -> {
                DeleteBucketPolicyRequest request = DeleteBucketPolicyRequest.builder().bucket(testBucket).build();
                return s3Async.deleteBucketPolicy(request);
            }, Expect.builder().build()),
            //data plane APIs
            new AsyncTestCase("ListObjectsV2-" + simpleName, () -> {
                ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(testBucket).build();
                return s3Async.listObjectsV2(request);
            }, Expect.builder().build()),
            new AsyncTestCase("DeleteObjects-" + simpleName, () -> {
                DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                                                                   .bucket(testBucket)
                                                                   .delete(Delete.builder()
                                                                                 .objects(ObjectIdentifier.builder()
                                                                                                          .key("fake")
                                                                                                          .build())
                                                                                 .build())
                                                                   .build();
                return s3Async.deleteObjects(request);
            }, Expect.builder().build()),
            new AsyncTestCase("HeadBucket-" + simpleName, () -> {
                HeadBucketRequest request = HeadBucketRequest.builder().bucket(testBucket).build();
                return s3Async.headBucket(request);
            }, Expect.builder().build())
        );
    }

    protected static void runAndVerify(SyncTestCase testCase) {
        Expect expectation = testCase.expectation();
        Runnable r = testCase.operationRunnable();

        if (expectation.error() != null) {
            assertThatThrownBy(r::run).hasMessageContaining(expectation.error());
        } else {
            try {
                r.run();
            } catch (Exception e) {
                fail("Could not call API", e);
            }
        }
        assertThat(capturingInterceptor.capturedRequests()).hasSize(1);
        capturingInterceptor.capturedRequests().forEach(req -> {
            Endpoint controlPlaneEndpoint = testCase.expectation().endpoint();
            if (controlPlaneEndpoint != null) {
                assertThat(req.getUri()).hasHost(controlPlaneEndpoint.url().getHost());
            } else {
                List<String> s3sessionTokenValue = req.headers().get("x-amz-s3session-token");
                assertThat(s3sessionTokenValue).isNotNull().hasSize(1);
            }
            if (testCase.toString().contains("PutBucketPolicy")) {
                List<String> sdkChecksumAlgorithm = req.headers().get("x-amz-sdk-checksum-algorithm");
                assertThat(sdkChecksumAlgorithm).isNotNull().hasSize(1).isEqualTo(Collections.singletonList("CRC32"));
                List<String> checksum = req.headers().get("x-amz-checksum-" + sdkChecksumAlgorithm.get(0).toLowerCase());
                assertThat(checksum).isNotNull();
            }
            List<String> contentSha256Value = req.headers().get("x-amz-content-sha256");
            assertThat(contentSha256Value).isNotNull().hasSize(1).isEqualTo(Collections.singletonList("UNSIGNED-PAYLOAD"));
        });
    }

    protected static void runAndVerify(AsyncTestCase testCase) {
        Expect expectation = testCase.expectation();
        Supplier<CompletableFuture<?>> r = testCase.operationRunnable();

        CompletableFuture<?> executeFuture = r.get();
        if (expectation.error() != null) {
            assertThatThrownBy(executeFuture::get).hasMessageContaining(expectation.error());
        } else {
            try {
                executeFuture.get();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                fail("Could not call API", e);
            }
        }
    }

    private static class CapturingInterceptor implements ExecutionInterceptor {
        private final List<SdkHttpRequest> capturedRequests = new ArrayList<>();
        private boolean isMd5Enabled;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            capturedRequests.add(context.httpRequest());
            isMd5Enabled = executionAttributes.getAttribute(CHECKSUM) != null;
        }

        public void reset() {
            capturedRequests.clear();
        }

        public List<SdkHttpRequest> capturedRequests() {
            return Collections.unmodifiableList(capturedRequests);
        }

        public boolean isMd5Enabled() {
            return isMd5Enabled;
        }
    }
}
