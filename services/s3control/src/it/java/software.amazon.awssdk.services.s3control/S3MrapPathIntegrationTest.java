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
import static org.assertj.core.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3control.model.CreateMultiRegionAccessPointInput;
import software.amazon.awssdk.services.s3control.model.GetMultiRegionAccessPointResponse;
import software.amazon.awssdk.services.s3control.model.ListMultiRegionAccessPointsResponse;
import software.amazon.awssdk.services.s3control.model.MultiRegionAccessPointStatus;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.utils.Logger;

public class S3MrapPathIntegrationTest extends S3ControlIntegrationTestBase {
    private static final Logger log = Logger.loggerFor(S3MrapPathIntegrationTest.class);

    private static final Region REGION = Region.US_WEST_2;
    private static String bucket;
    private static String mrapName;

    private static final int RETRY_TIMES = 10;
    private static final int RETRY_DELAY_IN_SECONDS = 30;

    private static S3ControlClient s3control;
    private static String mrapAlias;
    private static StsClient stsClient;
    private static S3Client s3Client;
    
    private static CaptureRequestInterceptor captureRequestInterceptor;

    @BeforeAll
    public static void setupFixture() {
        captureRequestInterceptor = new CaptureRequestInterceptor();
        s3control = S3ControlClient.builder()
                                   .region(REGION)
                                   .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                   .build();

        s3Client = S3Client.builder()
                           .region(REGION)
                           .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                           .serviceConfiguration(S3Configuration.builder().useArnRegionEnabled(true).build())
                           .overrideConfiguration(ClientOverrideConfiguration.builder()
                                                      .addExecutionInterceptor(captureRequestInterceptor)
                                                                             .build())
                           .build();

        stsClient = StsClient.builder()
                             .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                             .region(REGION)
                             .build();
        accountId = getAccountId();
        bucket = "do-not-delete-s3mraptest-" + accountId;
        mrapName = "javaintegtest" + accountId;
        log.info(() -> "bucket " + bucket);

        createMrapIfNotExist(accountId, mrapName);

        mrapAlias = getMrapAliasAndVerify(accountId);
    }

    @ParameterizedTest(name = "{index}:key = {1},       {0}")
    @MethodSource("keys")
    public void whenCallingS3MrapWithDifferentPaths_withoutPathNormalization_requestIsAccepted(String name, String key, String expected) {
        deleteObjectIfExists(s3Client, bucket, key);
        s3Client.putObject(r -> r.bucket(bucket).key(key), RequestBody.fromString("CONTENT"));
        s3Client.getObjectAsBytes(r -> r.bucket(bucket).key(key)).asString(StandardCharsets.UTF_8);
        try {
            s3Client.getObjectAsBytes(r -> r.bucket(bucket).key(key)).asString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertThat(captureRequestInterceptor.normalizePath).isNotNull().isEqualTo(false);
        assertThat(captureRequestInterceptor.request.encodedPath()).isEqualTo(expected);

        try {
            deleteObjectIfExists(s3Client, constructMrapArn(accountId, mrapAlias), key);
            s3Client.putObject(r -> r.bucket(constructMrapArn(accountId, mrapAlias)).key(key), RequestBody.fromString("CONTENT"));
            s3Client.getObjectAsBytes(r -> r.bucket(constructMrapArn(accountId, mrapAlias)).key(key)).asString(StandardCharsets.UTF_8);
        } catch (S3Exception e) {
            fail(e.getMessage());
        }
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
            // Arguments.of("Single segment, double dot -> unchanged", "/..", "/.."), S3 does not accept this path
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
        String format = String.format("arn:aws:s3::%s:accesspoint:%s", account, mrapAlias);
        return format;
    }

    private void deleteObjectIfExists(S3Client s31, String bucket1, String key1) {
        try {
            s31.deleteObject(r -> r.bucket(bucket1).key(key1));
        } catch (NoSuchKeyException e) {
        }
    }

    private static String getAccountId() {
        return stsClient.getCallerIdentity().account();
    }

    private static String getMrapAliasAndVerify(String accountId) {
        GetMultiRegionAccessPointResponse mrap = s3control.getMultiRegionAccessPoint(r -> r.accountId(accountId).name(mrapName));
        assertThat(mrap.accessPoint()).isNotNull();
        assertThat(mrap.accessPoint().name()).isEqualTo(mrapName);
        log.info(() -> "Alias: " + mrap.accessPoint().alias());
        return mrap.accessPoint().alias();
    }

    private static void createMrapIfNotExist(String account, String mrapName) {
        software.amazon.awssdk.services.s3control.model.Region mrapRegion =
            software.amazon.awssdk.services.s3control.model.Region.builder().bucket(bucket).build();

        if (s3control.listMultiRegionAccessPoints(r -> r.accountId(account))
                     .accessPoints().stream().noneMatch(a -> a.name().equals(S3MrapPathIntegrationTest.mrapName))) {
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

    private static class CaptureRequestInterceptor implements ExecutionInterceptor {

        private SdkHttpRequest request;

        private Boolean normalizePath;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.request = context.httpRequest();
            this.normalizePath = executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_NORMALIZE_PATH);
        }
    }
}
