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
package software.amazon.awssdk.services.s3control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.StringUtils.isEmpty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.internal.SignerConstant;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.plugins.S3OverrideAuthSchemePropertiesPlugin;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3control.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3control.model.CreateMultiRegionAccessPointInput;
import software.amazon.awssdk.services.s3control.model.GetMultiRegionAccessPointResponse;
import software.amazon.awssdk.services.s3control.model.ListMultiRegionAccessPointsResponse;
import software.amazon.awssdk.services.s3control.model.MultiRegionAccessPointStatus;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringInputStream;

public class S3MrapIntegrationTest extends S3ControlIntegrationTestBase {
    private static final Logger log = Logger.loggerFor(S3MrapIntegrationTest.class);

    private static final String SIGV4A_CHUNKED_PAYLOAD_SIGNING = "STREAMING-AWS4-ECDSA-P256-SHA256-PAYLOAD-TRAILER";
    private static final String SIGV4_CHUNKED_PAYLOAD_SIGNING = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER";
    private static final String STREAMING_UNSIGNED_PAYLOAD_TRAILER = "STREAMING-UNSIGNED-PAYLOAD-TRAILER";
    private static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";
    private static final Region REGION = Region.US_WEST_2;
    private static String bucket;
    private static String mrapName;
    private static final String KEY = "aws-java-sdk-small-test-object";
    private static final String CONTENT = "A short string for a small test object";
    private static final int RETRY_TIMES = 10;
    private static final int RETRY_DELAY_IN_SECONDS = 30;

    private static S3ControlClient s3control;
    private static CaptureRequestInterceptor captureInterceptor;
    private static String mrapAlias;
    private static StsClient stsClient;
    private static S3Client s3Client;
    private static S3Client s3ClientWithPayloadSigning;

    @BeforeAll
    public static void setupFixture() {
        captureInterceptor = new CaptureRequestInterceptor();

        s3control = S3ControlClient.builder()
                                   .region(REGION)
                                   .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                   .build();

        s3Client = mrapEnabledS3Client(Collections.singletonList(captureInterceptor));
        s3ClientWithPayloadSigning = mrapEnabledS3ClientWithPayloadSigning(captureInterceptor);

        stsClient = StsClient.builder()
                             .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                             .region(REGION)
                             .build();
        accountId = stsClient.getCallerIdentity().account();
        bucket = "do-not-delete-s3mraptest-" + accountId;
        mrapName = "javaintegtest" + accountId;
        log.info(() -> "bucket " + bucket);

        createBucketIfNotExist(bucket);
        createMrapIfNotExist(accountId, mrapName);
        mrapAlias = getMrapAliasAndVerify(accountId, mrapName);
    }

    @ParameterizedTest(name = "{index}:key = {1},       {0}")
    @MethodSource("keys")
    public void when_callingMrapWithDifferentPaths_unsignedPayload_requestIsAccepted(String name, String key, String expected) {
        putGetDeleteObjectMrap(s3Client, STREAMING_UNSIGNED_PAYLOAD_TRAILER, key, expected);
    }

    @ParameterizedTest(name = "{index}:key = {1},       {0}")
    @MethodSource("keys")
    public void when_callingMrapWithDifferentPaths_signedPayload_requestIsAccepted(String name, String key, String expected) {
        putGetDeleteObjectMrap(s3ClientWithPayloadSigning, SIGV4A_CHUNKED_PAYLOAD_SIGNING, key, expected);
    }

    @ParameterizedTest(name = "{index}:key = {1},       {0}")
    @MethodSource("keys")
    public void when_callingS3WithDifferentPaths_unsignedPayload_requestIsAccepted(String name, String key, String expected) {
        putGetDeleteObjectStandard(s3Client, STREAMING_UNSIGNED_PAYLOAD_TRAILER, key, expected);
    }

    @ParameterizedTest(name = "{index}:key = {1},       {0}")
    @MethodSource("keys")
    public void when_callingS3WithDifferentPaths_signedPayload_requestIsAccepted(String name, String key, String expected) {
        putGetDeleteObjectStandard(s3ClientWithPayloadSigning, SIGV4_CHUNKED_PAYLOAD_SIGNING, key, expected);
    }

    @Test
    public void when_creatingPresignedMrapUrl_getRequestWorks() {
        S3Presigner presigner = s3Presigner();
        PresignedGetObjectRequest presignedGetObjectRequest =
            presigner.presignGetObject(p -> p.getObjectRequest(r -> r.bucket(constructMrapArn(accountId, mrapAlias)).key(KEY))
                                             .signatureDuration(Duration.ofMinutes(10)));

        deleteObjectIfExists(s3Client, constructMrapArn(accountId, mrapAlias), KEY);
        s3Client.putObject(r -> r.bucket(constructMrapArn(accountId, mrapAlias)).key(KEY), RequestBody.fromString(CONTENT));

        String object = applyPresignedUrl(presignedGetObjectRequest, null);
        assertEquals(CONTENT, object);
        verifySigv4aRequest(captureInterceptor.request(), STREAMING_UNSIGNED_PAYLOAD_TRAILER);
    }

    public void putGetDeleteObjectMrap(S3Client testClient, String payloadSigningTag, String key, String expected) {
        deleteObjectIfExists(testClient, constructMrapArn(accountId, mrapAlias), key);
        testClient.putObject(r -> r.bucket(constructMrapArn(accountId, mrapAlias)).key(key), RequestBody.fromString(CONTENT));
        verifySigv4aRequest(captureInterceptor.request(), payloadSigningTag);

        String object = testClient.getObjectAsBytes(r -> r.bucket(constructMrapArn(accountId, mrapAlias)).key(key)).asString(StandardCharsets.UTF_8);
        assertEquals(CONTENT, object);
        verifySigv4aRequest(captureInterceptor.request(), UNSIGNED_PAYLOAD);

        testClient.deleteObject(r -> r.bucket(constructMrapArn(accountId, mrapAlias)).key(key));
        verifySigv4aRequest(captureInterceptor.request(), UNSIGNED_PAYLOAD);

        assertThat(captureInterceptor.normalizePath).isNotNull().isEqualTo(false);
        assertThat(captureInterceptor.request.encodedPath()).isEqualTo(expected);
    }

    public void putGetDeleteObjectStandard(S3Client testClient, String payloadSigningTag, String key, String expected) {
        deleteObjectIfExists(testClient, bucket, key);
        testClient.putObject(r -> r.bucket(bucket).key(key), RequestBody.fromString(CONTENT));
        verifySigv4Request(captureInterceptor.request(), payloadSigningTag);

        String object = testClient.getObjectAsBytes(r -> r.bucket(bucket).key(key)).asString(StandardCharsets.UTF_8);
        assertEquals(CONTENT, object);
        verifySigv4Request(captureInterceptor.request(), UNSIGNED_PAYLOAD);

        testClient.deleteObject(r -> r.bucket(bucket).key(key));
        verifySigv4Request(captureInterceptor.request(), UNSIGNED_PAYLOAD);

        assertThat(captureInterceptor.normalizePath).isNotNull().isEqualTo(false);
        assertThat(captureInterceptor.request.encodedPath()).isEqualTo(expected);
    }

    private void verifySigv4aRequest(SdkHttpRequest signedRequest, String payloadSigningTag) {
        assertThat(signedRequest.headers().get("Authorization").get(0)).contains("AWS4-ECDSA-P256-SHA256");
        assertThat(signedRequest.headers().get("Host").get(0)).isEqualTo(constructMrapHostname(mrapAlias));
        assertThat(signedRequest.headers().get("x-amz-content-sha256").get(0)).isEqualTo(payloadSigningTag);
        assertThat(signedRequest.headers().get("X-Amz-Date").get(0)).isNotEmpty();
        assertThat(signedRequest.headers().get("X-Amz-Region-Set").get(0)).isEqualTo("*");
    }

    private void verifySigv4Request(SdkHttpRequest signedRequest, String payloadSigningTag) {
        assertThat(signedRequest.headers().get("Authorization").get(0)).contains(SignerConstant.AWS4_SIGNING_ALGORITHM);
        assertThat(signedRequest.headers().get("Host").get(0)).isEqualTo(String.format("%s.s3.%s.amazonaws.com",
                                                                                       bucket, REGION.id()));
        assertThat(signedRequest.headers().get("x-amz-content-sha256").get(0)).isEqualTo(payloadSigningTag);
        assertThat(signedRequest.headers().get("X-Amz-Date").get(0)).isNotEmpty();
    }

    private static Stream<Arguments> keys() {
        return Stream.of(
            Arguments.of("Slash -> unchanged", "/", "//"),
            Arguments.of("Single segment with initial slash -> unchanged", "/foo", "//foo"),
            Arguments.of("Single segment no slash -> slash prepended", "foo", "/foo"),
            Arguments.of("Multiple segments -> unchanged", "/foo/bar", "//foo/bar"),
            Arguments.of("Multiple segments with trailing slash -> unchanged", "/foo/bar/", "//foo/bar/"),
            Arguments.of("Multiple segments, urlEncoded slash -> encodes percent", "/foo%2Fbar", "//foo%252Fbar"),
            Arguments.of("Single segment, dot -> should remove dot", "/.", "//."),
            Arguments.of("Multiple segments with dot -> should remove dot", "/foo/./bar", "//foo/./bar"),
            Arguments.of("Multiple segments with ending dot -> should remove dot and trailing slash", "/foo/bar/.", "//foo/bar/."),
            Arguments.of("Multiple segments with dots -> should remove dots and preceding segment", "/foo/bar/../baz", "//foo/bar/../baz"),
            Arguments.of("First segment has colon -> unchanged, url encoded first", "foo:/bar", "/foo%3A/bar"),
            Arguments.of("Multiple segments, urlEncoded slash -> encodes percent", "/foo%2F.%2Fbar", "//foo%252F.%252Fbar"),
            Arguments.of("No url encode, Multiple segments with dot -> unchanged", "/foo/./bar", "//foo/./bar"),
            Arguments.of("Multiple segments with dots -> unchanged", "/foo/bar/../baz", "//foo/bar/../baz"),
            Arguments.of("double slash", "//H", "///H"),
            Arguments.of("double slash in middle", "A//H", "/A//H")
        );
    }

    private String constructMrapArn(String account, String mrapAlias) {
        return String.format("arn:aws:s3::%s:accesspoint:%s", account, mrapAlias);
    }

    private String constructMrapHostname(String mrapAlias) {
        return String.format("%s.accesspoint.s3-global.amazonaws.com", mrapAlias);
    }

    private S3Presigner s3Presigner() {
        return S3Presigner.builder()
                          .region(REGION)
                          .serviceConfiguration(S3Configuration.builder()
                                                               .useArnRegionEnabled(true)
                                                               .checksumValidationEnabled(false)
                                                               .build())
                          .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                          .build();
    }

    private static void createMrapIfNotExist(String account, String mrapName) {
        software.amazon.awssdk.services.s3control.model.Region mrapRegion =
            software.amazon.awssdk.services.s3control.model.Region.builder().bucket(bucket).build();

        boolean mrapNotExists = s3control.listMultiRegionAccessPoints(r -> r.accountId(account))
                                         .accessPoints().stream()
                                         .noneMatch(a -> a.name().equals(S3MrapIntegrationTest.mrapName));
        if (mrapNotExists) {
            CreateMultiRegionAccessPointInput details = CreateMultiRegionAccessPointInput.builder()
                                                                                         .name(mrapName)
                                                                                         .regions(mrapRegion)
                                                                                         .build();
            log.info(() -> "Creating MRAP: " + mrapName);
            s3control.createMultiRegionAccessPoint(r -> r.accountId(account).details(details));
            waitForResourceCreation(mrapName);
        }
    }

    private static void waitForResourceCreation(String mrapName) throws IllegalStateException {
        Waiter<ListMultiRegionAccessPointsResponse> waiter =
            Waiter.builder(ListMultiRegionAccessPointsResponse.class)
                  .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(r ->
                      r.accessPoints().stream().findFirst().filter(mrap -> mrap.name().equals(mrapName) && mrap.status().equals(MultiRegionAccessPointStatus.READY)).isPresent()
                  ))
                  .addAcceptor(WaiterAcceptor.retryOnResponseAcceptor(i -> true))
                .overrideConfiguration(b -> b.maxAttempts(RETRY_TIMES).backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofSeconds(RETRY_DELAY_IN_SECONDS))))
                .build();

        waiter.run(() -> s3control.listMultiRegionAccessPoints(r -> r.accountId(accountId)));
    }

    public static String getMrapAliasAndVerify(String account, String mrapName) {
        GetMultiRegionAccessPointResponse mrap = s3control.getMultiRegionAccessPoint(r -> r.accountId(account).name(mrapName));
        assertThat(mrap.accessPoint()).isNotNull();
        assertThat(mrap.accessPoint().name()).isEqualTo(mrapName);
        log.info(() -> "Alias: " + mrap.accessPoint().alias());
        return mrap.accessPoint().alias();
    }

    private String applyPresignedUrl(PresignedRequest presignedRequest, String content) {
        try {
            HttpExecuteRequest.Builder builder = HttpExecuteRequest.builder().request(presignedRequest.httpRequest());
            if (!isEmpty(content)) {
                builder.contentStreamProvider(() -> new StringInputStream(content));
            }
            HttpExecuteRequest request = builder.build();
            HttpExecuteResponse response = ApacheHttpClient.create().prepareRequest(request).call();
            return response.responseBody()
                           .map(stream -> invokeSafely(() -> IoUtils.toUtf8String(stream)))
                           .orElseThrow(() -> new IOException("No input stream"));
        } catch (IOException e) {
            log.error(() -> "Error occurred ", e);
        }
        return null;
    }

    private static S3Client mrapEnabledS3Client(List<ExecutionInterceptor> executionInterceptors) {
        return S3Client.builder()
                       .region(REGION)
                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                       .serviceConfiguration(S3Configuration.builder()
                                                            .useArnRegionEnabled(true)
                                                            .build())
                       .overrideConfiguration(o -> o.executionInterceptors(executionInterceptors))
                       .build();
    }

    private static S3Client mrapEnabledS3ClientWithPayloadSigning(ExecutionInterceptor executionInterceptor) {
        // We can't use here `S3OverrideAuthSchemePropertiesPlugin.enablePayloadSigningPlugin()` since
        // it enables payload signing for *all* operations.
        SdkPlugin plugin = S3OverrideAuthSchemePropertiesPlugin.builder()
                                                               .payloadSigningEnabled(true)
                                                               .addOperationConstraint("UploadPart")
                                                               .addOperationConstraint("PutObject")
                                                               .build();
        return S3Client.builder()
                       .region(REGION)
                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                       .serviceConfiguration(S3Configuration.builder()
                                                            .useArnRegionEnabled(true)
                                                            .build())
                       .overrideConfiguration(o -> o.addExecutionInterceptor(executionInterceptor))
                       .addPlugin(plugin)
                       .build();
    }

    private void deleteObjectIfExists(S3Client s31, String bucket1, String key) {
        System.out.println(bucket1);
        try {
            s31.deleteObject(r -> r.bucket(bucket1).key(key));
        } catch (NoSuchKeyException e) {
        }
    }

    private static void createBucketIfNotExist(String bucket) {
        try {
            s3Client.createBucket(b -> b.bucket(bucket));
            s3Client.waiter().waitUntilBucketExists(b -> b.bucket(bucket));
        } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException e) {
            // ignore
        }
    }

    private static class CaptureRequestInterceptor implements ExecutionInterceptor {

        private SdkHttpRequest request;
        private Boolean normalizePath;

        public SdkHttpRequest request() {
            return request;
        }

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.request = context.httpRequest();
            this.normalizePath = executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_NORMALIZE_PATH);
        }
    }
}
