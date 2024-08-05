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

package software.amazon.awssdk.services.s3.internal.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;

class MultipartClientUserAgentTest {
    private MockAsyncHttpClient mockAsyncHttpClient;
    private UserAgentInterceptor userAgentInterceptor;
    private S3AsyncClient s3Client;

    @BeforeEach
    void init() {
        this.mockAsyncHttpClient = new MockAsyncHttpClient();
        this.userAgentInterceptor = new UserAgentInterceptor();
        s3Client = S3AsyncClient.builder()
                                .httpClient(mockAsyncHttpClient)
                                .endpointOverride(URI.create("http://localhost"))
                                .overrideConfiguration(c -> c.addExecutionInterceptor(userAgentInterceptor))
                                .multipartConfiguration(c -> c.minimumPartSizeInBytes(512L).thresholdInBytes(512L))
                                .multipartEnabled(true)
                                .region(Region.US_EAST_1)
                                .build();
    }

    @AfterEach
    void reset() {
        this.mockAsyncHttpClient.reset();
    }

    @Test
    void validateUserAgent_nonMultipartMethod() throws Exception {
        HttpExecuteResponse response = HttpExecuteResponse.builder()
                                                          .response(SdkHttpResponse.builder().statusCode(200).build())
                                                          .build();
        mockAsyncHttpClient.stubResponses(response);

        s3Client.headObject(req -> req.key("mock").bucket("mock")).get();

        assertThat(userAgentInterceptor.apiNames)
            .anyMatch(api -> "hll".equals(api.name()) && "s3Multipart".equals(api.version()));
    }

    private static final class UserAgentInterceptor implements ExecutionInterceptor {
        private final List<ApiName> apiNames = new ArrayList<>();

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            context.request().overrideConfiguration().ifPresent(c -> apiNames.addAll(c.apiNames()));
        }
    }

}
