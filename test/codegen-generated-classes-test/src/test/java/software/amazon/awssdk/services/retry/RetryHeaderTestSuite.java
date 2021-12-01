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

package software.amazon.awssdk.services.retry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.testutils.service.http.MockHttpClient;

/**
 * A set of tests that verify the behavior of retry-related headers (amz-sdk-invocation-id and amz-sdk-request).
 */
public abstract class RetryHeaderTestSuite<T extends MockHttpClient> {
    protected final T mockHttpClient;

    protected RetryHeaderTestSuite(T mockHttpClient) {
        this.mockHttpClient = mockHttpClient;
    }

    @BeforeEach
    public void setupClient() {
        mockHttpClient.reset();
    }

    protected abstract void callAllTypesOperation();

    @Test
    public void invocationIdSharedBetweenRetries() {
        mockHttpClient.stubResponses(retryableFailure(), retryableFailure(), success());

        callAllTypesOperation();

        List<SdkHttpRequest> requests = mockHttpClient.getRequests();

        assertThat(requests).hasSize(3);
        String firstInvocationId = invocationId(requests.get(0));
        assertThat(invocationId(requests.get(1))).isEqualTo(firstInvocationId);
        assertThat(invocationId(requests.get(2))).isEqualTo(firstInvocationId);
    }

    @Test
    public void invocationIdDifferentBetweenApiCalls() {
        mockHttpClient.stubResponses(success());

        callAllTypesOperation();
        callAllTypesOperation();

        List<SdkHttpRequest> requests = mockHttpClient.getRequests();

        assertThat(requests).hasSize(2);
        String firstInvocationId = invocationId(requests.get(0));
        assertThat(invocationId(requests.get(1))).isNotEqualTo(firstInvocationId);
    }

    @Test
    public void retryAttemptAndMaxAreCorrect() {
        mockHttpClient.stubResponses(retryableFailure(), success());

        callAllTypesOperation();

        List<SdkHttpRequest> requests = mockHttpClient.getRequests();

        assertThat(requests).hasSize(2);
        assertThat(retryComponent(requests.get(0), "attempt")).isEqualTo("1");
        assertThat(retryComponent(requests.get(1), "attempt")).isEqualTo("2");
        assertThat(retryComponent(requests.get(0), "max")).isEqualTo("3");
        assertThat(retryComponent(requests.get(1), "max")).isEqualTo("3");
    }

    private String invocationId(SdkHttpRequest request) {
        return request.firstMatchingHeader("amz-sdk-invocation-id")
                      .orElseThrow(() -> new AssertionError("Expected aws-sdk-invocation-id in " + request));
    }

    private String retryComponent(SdkHttpRequest request, String componentName) {
        return retryComponent(request.firstMatchingHeader("amz-sdk-request")
                                     .orElseThrow(() -> new AssertionError("Expected amz-sdk-request in " + request)),
                              componentName);
    }

    private String retryComponent(String amzSdkRequestHeader, String componentName) {
        return Stream.of(amzSdkRequestHeader.split(";"))
                     .map(h -> h.split("="))
                     .filter(h -> h[0].trim().equals(componentName))
                     .map(h -> h[1].trim())
                     .findAny()
                     .orElseThrow(() -> new AssertionError("Expected " + componentName + " in " + amzSdkRequestHeader));
    }

    private HttpExecuteResponse retryableFailure() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(500)
                                                           .putHeader("content-length", "0")
                                                           .build())
                                  .build();
    }

    private HttpExecuteResponse success() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(200)
                                                           .putHeader("content-length", "0")
                                                           .build())
                                  .build();
    }
}
