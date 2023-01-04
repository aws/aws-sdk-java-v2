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
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.testutils.service.http.MockHttpClient;

/**
 * A set of tests that verify the behavior of the SDK when retries are exhausted.
 */
public abstract class RetryFailureTestSuite<T extends MockHttpClient> {
    protected final T mockHttpClient;

    protected RetryFailureTestSuite(T mockHttpClient) {
        this.mockHttpClient = mockHttpClient;
    }

    @BeforeEach
    public void setupClient() {
        mockHttpClient.reset();
    }

    protected abstract void callAllTypesOperation();

    @Test
    public void clientSideErrorsIncludeSuppressedExceptions() {
        mockHttpClient.stubResponses(retryableFailure(),
                                     retryableFailure(),
                                     nonRetryableFailure());

        try {
            callAllTypesOperation();
        } catch (Throwable e) {
            if (e instanceof CompletionException) {
                e = e.getCause();
            }
            e.printStackTrace();

            assertThat(e.getSuppressed()).hasSize(2)
                                         .allSatisfy(t -> assertThat(t.getMessage()).contains("500"));
        }
    }

    private HttpExecuteResponse retryableFailure() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(500)
                                                           .putHeader("content-length", "0")
                                                           .build())
                                  .build();
    }

    private HttpExecuteResponse nonRetryableFailure() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(400)
                                                           .putHeader("content-length", "0")
                                                           .build())
                                  .build();
    }
}
