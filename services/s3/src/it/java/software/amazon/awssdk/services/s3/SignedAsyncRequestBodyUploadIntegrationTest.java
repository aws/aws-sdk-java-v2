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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.SIGNER;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.signer.AsyncAws4Signer;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.AsyncSigner;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpHeaders;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.utils.S3TestUtils;

/**
 * This is an integration test to verify that {@link AsyncAws4Signer} is able to correctly sign async requests that
 * have a streaming payload.
 */
public class SignedAsyncRequestBodyUploadIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET = "signed-body-test-" + System.currentTimeMillis();
    private static S3AsyncClient testClient;

    private static TestSigner mockSigner;
    private static final CapturingInterceptor capturingInterceptor = new CapturingInterceptor();

    @BeforeClass
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();

        // Use a mock so we can introspect easily to verify that the signer was used for the request
        mockSigner = mock(TestSigner.class);

        AsyncAws4Signer realSigner = AsyncAws4Signer.create();

        when(mockSigner.sign(any(SdkHttpFullRequest.class), any(AsyncRequestBody.class), any(ExecutionAttributes.class)))
                .thenAnswer(i -> {
                    SdkHttpFullRequest request = i.getArgumentAt(0, SdkHttpFullRequest.class);
                    AsyncRequestBody body = i.getArgumentAt(1, AsyncRequestBody.class);
                    ExecutionAttributes executionAttributes = i.getArgumentAt(2, ExecutionAttributes.class);
                    return realSigner.sign(request, body, executionAttributes);
                });

        testClient = s3AsyncClientBuilder()
                .overrideConfiguration(o -> o
                        .putAdvancedOption(SIGNER, mockSigner)
                        .addExecutionInterceptor(capturingInterceptor))
                .build();

        createBucket(BUCKET);
    }

    @AfterClass
    public static void teardown() {
        S3TestUtils.deleteBucketAndAllContents(s3, BUCKET);
        s3.close();
        s3Async.close();
        testClient.close();
    }

    @Before
    public void methodSetup() {
        capturingInterceptor.reset();
    }

    @Test
    public void test_putObject_bodyIsSigned_succeeds() {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET).key("test.txt")
                // Instructs the signer to include the SHA-256 of the body as a header; bit weird but that's how it's
                // done
                // See https://github.com/aws/aws-sdk-java-v2/blob/aeb4b5853c8f833f266110f1e01d6e10ea6ac1c5/core/auth/src/main/java/software/amazon/awssdk/auth/signer/internal/AbstractAws4Signer.java#L75-L77
                .overrideConfiguration(o -> o.putHeader("x-amz-content-sha256", "required"))
                .build();

        testClient.putObject(request, AsyncRequestBody.fromString("Hello S3")).join();

        // Ensure that the client used our signer
        verify(mockSigner, atLeastOnce()).sign(
                any(SdkHttpFullRequest.class), any(AsyncRequestBody.class), any(ExecutionAttributes.class));

        List<String> capturedSha256Values = capturingInterceptor.capturedRequests().stream()
                .map(SdkHttpHeaders::headers)
                .map(m -> m.getOrDefault("x-amz-content-sha256", Collections.emptyList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertThat(capturedSha256Values)
                // echo -n 'Hello S3' | shasum -a 256
                .containsExactly("c9f7ed78c073c16bcb2f76fa4a5739cb6cf81677d32fdbeda1d69350d107b6f3");
    }

    private interface TestSigner extends AsyncSigner, Signer {
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
}
