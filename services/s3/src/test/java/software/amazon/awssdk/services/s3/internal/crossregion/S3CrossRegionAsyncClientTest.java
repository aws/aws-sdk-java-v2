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
import static software.amazon.awssdk.services.s3.internal.crossregion.S3DecoratorRedirectBaseTest.CROSS_REGION;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3DecoratorRedirectBaseTest.X_AMZ_BUCKET_REGION;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
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
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.endpoints.internal.DefaultS3EndpointProvider;
import software.amazon.awssdk.services.s3.internal.crossregion.endpointprovider.BucketEndpointProvider;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Publisher;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.StringUtils;

class S3CrossRegionAsyncClientTest {

    private static final String RESPONSE = "<Res>response</Res>";
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final String TOKEN = "token";

    private final MockAsyncHttpClient mockAsyncHttpClient = new MockAsyncHttpClient();
    private CaptureInterceptor captureInterceptor;
    private S3AsyncClient s3Client;

    @BeforeEach
    void before() {
        captureInterceptor = new CaptureInterceptor();
        s3Client = clientBuilder().build();
    }

    @Test
    void standardOp_crossRegionClient_noOverrideConfig_SuccessfullyIntercepts() {
        mockAsyncHttpClient.stubResponses(customHttpResponse(301, CROSS_REGION), successHttpResponse());
        S3AsyncClient crossRegionClient = new S3CrossRegionAsyncClient(s3Client);
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);
    }

    @Test
    void standardOp_crossRegionClient_existingOverrideConfig_SuccessfullyIntercepts() {
        mockAsyncHttpClient.stubResponses(customHttpResponse(301, CROSS_REGION), successHttpResponse());
        S3AsyncClient crossRegionClient = new S3CrossRegionAsyncClient(s3Client);
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket(BUCKET)
                                                   .key(KEY)
                                                   .overrideConfiguration(o -> o.putHeader("someheader", "somevalue"))
                                                   .build();
        crossRegionClient.getObject(request, AsyncResponseTransformer.toBytes()).join();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);
        assertThat(mockAsyncHttpClient.getLastRequest().headers().get("someheader")).isNotNull();
    }

    @Test
    void paginatedOp_crossRegionClient_DoesIntercept() throws Exception {
        mockAsyncHttpClient.stubResponses(customHttpResponse(301, CROSS_REGION), successHttpResponse());
        S3AsyncClient crossRegionClient = new S3CrossRegionAsyncClient(s3Client);
        ListObjectsV2Publisher publisher =
            crossRegionClient.listObjectsV2Paginator(r -> r.bucket(BUCKET).continuationToken(TOKEN).build());
        CompletableFuture<Void> future = publisher.subscribe(ListObjectsV2Response::contents);
        future.get();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);
    }

    @Test
    void crossRegionClient_createdWithWrapping_SuccessfullyIntercepts() {
        mockAsyncHttpClient.stubResponses(customHttpResponse(301, CROSS_REGION), successHttpResponse());
        S3AsyncClient crossRegionClient = clientBuilder().serviceConfiguration(c -> c.crossRegionAccessEnabled(true)).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);
    }

    private S3AsyncClientBuilder clientBuilder() {
        return S3AsyncClient.builder()
                            .httpClient(mockAsyncHttpClient)
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


    public static HttpExecuteResponse successHttpResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(200)
                                                           .build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(RESPONSE)))
                                  .build();
    }

    public static HttpExecuteResponse customHttpResponse(int statusCode, String bucket_region) {
        SdkHttpFullResponse.Builder httpResponseBuilder = SdkHttpResponse.builder();
        if (StringUtils.isNotBlank(bucket_region)) {
            httpResponseBuilder.appendHeader(X_AMZ_BUCKET_REGION, bucket_region);
        }
        return HttpExecuteResponse.builder()
                                  .response(httpResponseBuilder.statusCode(statusCode).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(RESPONSE)))
                                  .build();
    }
}
