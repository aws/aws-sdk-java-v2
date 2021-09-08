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

package software.amazon.awssdk.services.s3.signer;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpHeaders;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.utils.S3TestUtils;

/**
 * This is an integration test to verify that payload signing for synchronous requests work as intended.
 */
public class PayloadSigningIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(PayloadSigningIntegrationTest.class);
    private static final String KEY = "key";

    private static final String SIGNED_PAYLOAD_HEADER_VALUE = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD";
    private static final String UNSIGNED_PAYLOAD_HEADER_VALUE = "UNSIGNED-PAYLOAD";

    private static final CapturingInterceptor capturingInterceptor = new CapturingInterceptor();

    @BeforeClass
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();
        createBucket(BUCKET);
    }

    @AfterClass
    public static void teardown() {
        S3TestUtils.deleteBucketAndAllContents(s3, BUCKET);
        s3.close();
    }

    @Before
    public void methodSetup() {
        capturingInterceptor.reset();
    }

    @Test
    public void standardSyncApacheHttpClient_unsignedPayload() {
        S3Client syncClient = s3ClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(capturingInterceptor))
            .build();
        assertThat(syncClient.putObject(b -> b.bucket(BUCKET).key(KEY),
                                        RequestBody.fromBytes("helloworld".getBytes()))).isNotNull();
        List<String> capturedSha256Values = getSha256Values();
        assertThat(capturedSha256Values).containsExactly(UNSIGNED_PAYLOAD_HEADER_VALUE);
        syncClient.close();
    }

    @Test
    public void standardSyncApacheHttpClient_httpCauses_signedPayload() {
        S3Client syncClient = s3ClientBuilder()
            .endpointOverride(URI.create("http://s3.us-west-2.amazonaws.com"))
            .overrideConfiguration(o -> o.addExecutionInterceptor(capturingInterceptor))
            .build();
        assertThat(syncClient.putObject(b -> b.bucket(BUCKET).key(KEY),
                                        RequestBody.fromBytes("helloworld".getBytes()))).isNotNull();
        List<String> capturedSha256Values = getSha256Values();
        assertThat(capturedSha256Values).containsExactly(SIGNED_PAYLOAD_HEADER_VALUE);
        syncClient.close();
    }

    @Test
    public void standardSyncApacheHttpClient_manuallyEnabled_signedPayload() {
        S3Client syncClient = s3ClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(capturingInterceptor)
                                         .addExecutionInterceptor(new PayloadSigningInterceptor()))
            .build();
        assertThat(syncClient.putObject(b -> b.bucket(BUCKET).key(KEY),
                                        RequestBody.fromBytes("helloworld".getBytes()))).isNotNull();
        List<String> capturedSha256Values = getSha256Values();
        assertThat(capturedSha256Values).containsExactly(SIGNED_PAYLOAD_HEADER_VALUE);
        syncClient.close();
    }

    private List<String> getSha256Values() {
        return capturingInterceptor.capturedRequests().stream()
                                   .map(SdkHttpHeaders::headers)
                                   .map(m -> m.getOrDefault("x-amz-content-sha256", Collections.emptyList()))
                                   .flatMap(Collection::stream).collect(Collectors.toList());
    }

    private static class CapturingInterceptor implements ExecutionInterceptor {
        private final List<SdkHttpRequest> capturedRequests = new ArrayList<>();

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            capturedRequests.add(context.httpRequest());
        }

        public void reset() {
            capturedRequests.clear();
        }

        public List<SdkHttpRequest> capturedRequests() {
            return capturedRequests;
        }
    }

    private static class PayloadSigningInterceptor implements ExecutionInterceptor {

        public Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context,
                                                       ExecutionAttributes executionAttributes) {
            executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, true);
            if (!context.requestBody().isPresent() && context.httpRequest().method().equals(SdkHttpMethod.POST)) {
                return Optional.of(RequestBody.fromBytes(new byte[0]));
            }

            return context.requestBody();
        }
    }
}
