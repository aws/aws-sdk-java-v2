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

package software.amazon.awssdk.core.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.time.Duration;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SdkSyncClientHandler;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import utils.HttpTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class InterruptFlagAlwaysClearsTest {
    private static final Duration SERVICE_LATENCY = Duration.ofMillis(10);

    private static SdkSyncClientHandler syncHttpClient;

    @Mock
    private Marshaller<SdkRequest> marshaller;

    @Mock
    private HttpResponseHandler<SdkResponse> responseHandler;

    @Mock
    private HttpResponseHandler<SdkServiceException> errorResponseHandler;

    @BeforeClass
    public static void setup() {
        SdkClientConfiguration clientConfiguration = HttpTestUtils.testClientConfiguration()
                .toBuilder()
                .option(SdkClientOption.SYNC_HTTP_CLIENT, new SleepyHttpClient())
                .option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT, SERVICE_LATENCY)
                .build();

        syncHttpClient = new TestClientHandler(clientConfiguration);
    }

    @Before
    public void methodSetup() throws Exception {
        when(marshaller.marshall(any(NoopTestRequest.class)))
                .thenReturn(SdkHttpFullRequest.builder()
                        .protocol("http")
                        .host("some-host.aws")
                        .method(SdkHttpMethod.GET)
                        .build());

        when(errorResponseHandler.handle(any(SdkHttpFullResponse.class), any(ExecutionAttributes.class)))
                .thenReturn(SdkServiceException.builder().message("BOOM").statusCode(400).build());
    }

    @AfterClass
    public static void cleanup() {
        syncHttpClient.close();
    }

    @Test
    public void interruptFlagClearsWithTimeoutCloseToLatency() {
        int numRuns = 100;

        int interruptCount = 0;

        for (int i = 0; i < numRuns; ++i) {
            // This request is expected to time out for some number of iterations
            executeRequestIgnoreErrors(syncHttpClient);

            // Check and clear interrupt flag
            if (Thread.interrupted()) {
                ++interruptCount;
            }
        }

        assertThat(interruptCount)
                .withFailMessage("Interrupt flags are leaking: %d of %d runs leaked interrupt flags.",
                                 interruptCount, numRuns)
                .isEqualTo(0);
    }

    private void executeRequestIgnoreErrors(SdkSyncClientHandler syncHttpClient) {
        try {
            syncHttpClient.execute(new ClientExecutionParams<SdkRequest, SdkResponse>()
                    .withOperationName("SomeOperation")
                    .withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler)
                    .withInput(NoopTestRequest.builder().build())
                    .withMarshaller(marshaller));
            Assert.fail();
        } catch (AbortedException | ApiCallAttemptTimeoutException | SdkServiceException e) {
            // Ignored
        }
    }

    private static class SleepyHttpClient implements SdkHttpClient {
        private static final HttpExecuteResponse RESPONSE = HttpExecuteResponse.builder()
                .response(SdkHttpResponse.builder()
                        .statusCode(400)
                        .build())
                .build();
        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() throws IOException {
                    try {
                        Thread.sleep(SERVICE_LATENCY.toMillis());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted!", e);
                    }

                    return RESPONSE;
                }

                @Override
                public void abort() {
                }
            };
        }

        @Override
        public void close() {

        }
    }

    private static class TestClientHandler extends SdkSyncClientHandler {
        protected TestClientHandler(SdkClientConfiguration clientConfiguration) {
            super(clientConfiguration);
        }
    }
}