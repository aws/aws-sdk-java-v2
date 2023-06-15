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

package software.amazon.awssdk.services.s3.internal.crossregion;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

class S3CrossRegionSyncClientTest {

    private static final String RESPONSE = "<Res>response</Res>";
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final String TOKEN = "token";

    private final MockSyncHttpClient mockSyncHttpClient = new MockSyncHttpClient();
    private CaptureInterceptor captureInterceptor;
    private S3Client defaultS3Client;

    @BeforeEach
    void before() {
        mockSyncHttpClient.stubNextResponse(
            HttpExecuteResponse.builder()
                               .response(SdkHttpResponse.builder().statusCode(200).build())
                               .responseBody(AbortableInputStream.create(new StringInputStream(RESPONSE)))
                               .build());

        captureInterceptor = new CaptureInterceptor();
        defaultS3Client = clientBuilder().build();
    }

    @Test
    void standardOp_crossRegionClient_noOverrideConfig_SuccessfullyIntercepts() {
        S3Client crossRegionClient = new S3CrossRegionSyncClient(defaultS3Client);
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(S3CrossRegionSyncClient.BucketEndpointProvider.class);
    }

    @Test
    void standardOp_crossRegionClient_existingOverrideConfig_SuccessfullyIntercepts() {
        S3Client crossRegionClient = new S3CrossRegionSyncClient(defaultS3Client);
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket(BUCKET)
                                                   .key(KEY)
                                                   .overrideConfiguration(o -> o.putHeader("someheader", "somevalue"))
                                                   .build();
        crossRegionClient.getObject(request);
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(S3CrossRegionSyncClient.BucketEndpointProvider.class);
        assertThat(mockSyncHttpClient.getLastRequest().headers().get("someheader")).isNotNull();
    }

    @Test
    void paginatedOp_crossRegionClient_DoesNotIntercept()  {
        S3Client crossRegionClient = new S3CrossRegionSyncClient(defaultS3Client);
        ListObjectsV2Iterable iterable =
            crossRegionClient.listObjectsV2Paginator(r -> r.bucket(BUCKET).continuationToken(TOKEN).build());
        iterable.forEach(ListObjectsV2Response::contents);
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(S3CrossRegionSyncClient.BucketEndpointProvider.class);
    }

    @Test
    void crossRegionClient_createdWithWrapping_SuccessfullyIntercepts() {
        S3Client crossRegionClient = clientBuilder().serviceConfiguration(c -> c.crossRegionAccessEnabled(true)).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(S3CrossRegionSyncClient.BucketEndpointProvider.class);
    }

    @Test
    void standardOp_crossRegionClient_containUserAgent() {
        S3Client crossRegionClient = clientBuilder().serviceConfiguration(c -> c.crossRegionAccessEnabled(true)).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        assertThat(mockSyncHttpClient.getLastRequest().firstMatchingHeader("User-Agent").get()).contains("hll/cross-region");
    }

    private S3ClientBuilder clientBuilder() {
        return S3Client.builder()
                       .httpClient(mockSyncHttpClient)
                       .endpointOverride(URI.create("http://localhost"))
                       .overrideConfiguration(c -> c.addExecutionInterceptor(captureInterceptor));
    }

    private static final class CaptureInterceptor implements ExecutionInterceptor {

        private EndpointProvider endpointProvider;

        @Override
        public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
            endpointProvider = executionAttributes.getAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER);
        }
    }
}
