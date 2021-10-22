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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.internal.SignerConstant;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3control.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3control.model.CreateMultiRegionAccessPointInput;
import software.amazon.awssdk.services.s3control.model.CreateMultiRegionAccessPointResponse;
import software.amazon.awssdk.services.s3control.model.GetMultiRegionAccessPointResponse;
import software.amazon.awssdk.services.s3control.model.ListMultiRegionAccessPointsResponse;
import software.amazon.awssdk.services.s3control.model.MultiRegionAccessPointStatus;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringInputStream;

public class S3MrapIntegrationTest extends S3ControlIntegrationTestBase {
    private static final Logger log = Logger.loggerFor(S3MrapIntegrationTest.class);

    private static final String CHUNKED_PAYLOAD_SIGNING = "STREAMING-AWS4-ECDSA-P256-SHA256-PAYLOAD";
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

    @BeforeClass
    public static void setupFixture() {
        captureInterceptor = new CaptureRequestInterceptor();

        s3control = S3ControlClient.builder()
                                   .region(REGION)
                                   .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                   .build();

        s3Client = S3Client.builder()
                           .region(REGION)
                           .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                           .build();

        stsClient = StsClient.builder()
                             .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                             .region(REGION)
                             .build();
        String accountId = getAccountId();
        bucket = "do-not-delete-s3mraptest-" + accountId;
        mrapName = "javaintegtest" + accountId;
        log.info(() -> "bucket " + bucket);
        createBucketIfNotExist(bucket);
        createMrapIfNotExist(mrapName);

        mrapAlias = getMrapAliasAndVerify();
    }

    private static void createBucketIfNotExist(String bucket) {
        try {
            s3Client.createBucket(b -> b.bucket(bucket));
            s3Client.waiter().waitUntilBucketExists(b -> b.bucket(bucket));
        } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException e) {
            // ignore
        }
    }

    private static String getAccountId() {
        return stsClient.getCallerIdentity().account();
    }

    public static String getMrapAliasAndVerify() {
        GetMultiRegionAccessPointResponse mrap = s3control.getMultiRegionAccessPoint(r -> r.accountId(accountId).name(mrapName));
        assertThat(mrap.accessPoint()).isNotNull();
        assertThat(mrap.accessPoint().name()).isEqualTo(mrapName);
        log.info(() -> "Alias: " + mrap.accessPoint().alias());
        return mrap.accessPoint().alias();
    }

    @Test
    public void object_lifecycle_workflow_through_mrap_unsigned_payload() {
        signingWorkflow(Collections.emptyList(), UNSIGNED_PAYLOAD);
    }

    @Test
    public void object_lifecycle_workflow_through_mrap_signed_payload() {
        signingWorkflow(Collections.singletonList(new PayloadSigningInterceptor()), CHUNKED_PAYLOAD_SIGNING);
    }

    public void signingWorkflow(List<ExecutionInterceptor> interceptors, String payloadSigningTag) {
        S3Client s3 = s3Client(interceptors);
        S3Presigner presigner = s3Presigner();

        listAndVerify(s3);
        putAndVerify(s3, payloadSigningTag);
        getAndVerify(s3);
        presignGetAndVerify(presigner);
        deleteAndVerify(s3);
    }

    private void listAndVerify(S3Client s3) {
        List<Bucket> buckets = s3.listBuckets().buckets();
        assertThat(buckets.stream().map(Bucket::name)).contains(bucket);
        verifySigv4SignedRequest(captureInterceptor.request());
    }

    private void putAndVerify(S3Client s3, String payloadSigningTag) {
        deleteObjectIfExists(s3);
        s3.putObject(r -> r.bucket(constructMrapArn(accountId, mrapAlias)).key(KEY), RequestBody.fromString(CONTENT));
        verifySigv4aRequest(captureInterceptor.request(), payloadSigningTag);
    }

    private void getAndVerify(S3Client s3) {
        String object =
            s3.getObjectAsBytes(r -> r.bucket(constructMrapArn(accountId, mrapAlias)).key(KEY)).asString(StandardCharsets.UTF_8);
        assertEquals(CONTENT, object);
        verifySigv4aRequest(captureInterceptor.request(), UNSIGNED_PAYLOAD);
    }

    private void presignGetAndVerify(S3Presigner presigner) {
        PresignedGetObjectRequest presignedGetObjectRequest =
            presigner.presignGetObject(p -> p.getObjectRequest(r -> r.bucket(constructMrapArn(accountId, mrapAlias)).key(KEY))
                                             .signatureDuration(Duration.ofMinutes(10)));
        String object = applyPresignedUrl(presignedGetObjectRequest, null);
        assertEquals(CONTENT, object);
        verifySigv4aRequest(captureInterceptor.request(), UNSIGNED_PAYLOAD);
    }

    private void deleteAndVerify(S3Client s3) {
        s3.deleteObject(r -> r.bucket(constructMrapArn(accountId, mrapAlias)).key(KEY));
        verifySigv4aRequest(captureInterceptor.request(), UNSIGNED_PAYLOAD);
    }

    private void verifySigv4aRequest(SdkHttpRequest signedRequest, String payloadSigningTag) {
        assertThat(signedRequest.headers().get("Authorization").get(0)).contains("AWS4-ECDSA-P256-SHA256");
        assertThat(signedRequest.headers().get("Host").get(0)).isEqualTo(constructMrapHostname(mrapAlias));
        assertThat(signedRequest.headers().get("x-amz-content-sha256").get(0)).isEqualTo(payloadSigningTag);
        assertThat(signedRequest.headers().get("X-Amz-Date").get(0)).isNotEmpty();
        assertThat(signedRequest.headers().get("X-Amz-Region-Set").get(0)).isEqualTo("*");
    }

    private void verifySigv4SignedRequest(SdkHttpRequest signedRequest) {
        assertThat(signedRequest.headers().get("Authorization").get(0)).contains(SignerConstant.AWS4_SIGNING_ALGORITHM);
        assertThat(signedRequest.headers().get("Host").get(0)).isEqualTo(String.format("s3.%s.amazonaws.com", REGION.id()));
        assertThat(signedRequest.headers().get("x-amz-content-sha256").get(0)).isEqualTo(UNSIGNED_PAYLOAD);
        assertThat(signedRequest.headers().get("X-Amz-Date").get(0)).isNotEmpty();
    }

    private String constructMrapArn(String account, String mrapAlias) {
        return String.format("arn:aws:s3::%s:accesspoint:%s", account, mrapAlias);
    }

    private String constructMrapHostname(String mrapAlias) {
        return String.format("%s.accesspoint.s3-global.amazonaws.com", mrapAlias);
    }

    private void deleteObjectIfExists(S3Client s3) {
        try {
            s3.deleteObject(r -> r.bucket(constructMrapArn(accountId, mrapAlias)).key(KEY));
        } catch (NoSuchKeyException e) {
        }
    }

    private S3Client s3Client(List<ExecutionInterceptor> interceptors) {
        List<ExecutionInterceptor> interceptorList = new ArrayList<>(interceptors);
        interceptorList.add(captureInterceptor);

        return S3Client.builder()
                       .region(REGION)
                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                       .serviceConfiguration(S3Configuration.builder()
                                                            .useArnRegionEnabled(true)
                                                            .build())
                       .overrideConfiguration(ClientOverrideConfiguration.builder()
                                                                         .executionInterceptors(interceptorList)
                                                                         .build())
                       .build();
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

    private static void createMrapIfNotExist(String mrapName) {
        software.amazon.awssdk.services.s3control.model.Region mrapRegion =
            software.amazon.awssdk.services.s3control.model.Region.builder().bucket(bucket).build();


        if (s3control.listMultiRegionAccessPoints(r -> r.accountId(accountId))
                      .accessPoints().stream().noneMatch(a -> a.name().equals(S3MrapIntegrationTest.mrapName))) {
            CreateMultiRegionAccessPointInput details = CreateMultiRegionAccessPointInput.builder()
                                                                                         .name(mrapName)
                                                                                         .regions(mrapRegion)
                                                                                         .build();
            log.info(() -> "Creating MRAP: " + mrapName);
            CreateMultiRegionAccessPointResponse response = s3control.createMultiRegionAccessPoint(r -> r.accountId(accountId)
                                                                                                         .details(details));
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

    private static class CaptureRequestInterceptor implements ExecutionInterceptor {

        private SdkHttpRequest request;

        public SdkHttpRequest request() {
            return request;
        }

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.request = context.httpRequest();
        }
    }

    private static class PayloadSigningInterceptor implements ExecutionInterceptor {

        public Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context,
                                                       ExecutionAttributes executionAttributes) {
            SdkRequest sdkRequest = context.request();

            if (sdkRequest instanceof PutObjectRequest || sdkRequest instanceof UploadPartRequest) {
                executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, true);
            }
            if (!context.requestBody().isPresent() && context.httpRequest().method().equals(SdkHttpMethod.POST)) {
                return Optional.of(RequestBody.fromBytes(new byte[0]));
            }

            return context.requestBody();
        }
    }
}
