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

package software.amazon.awssdk.core.internal.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.internal.util.ResponseHandlerTestUtils.combinedSyncResponseHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Pair;
import utils.HttpTestUtils;
import utils.ValidSdkObjects;

/**
 * Runs a request through the full sync pipeline to verify the order and effect of the response interceptor hooks.
 */
class AmazonSyncHttpClientResponseInterceptorTest {

    private AmazonSyncHttpClient client;

    @BeforeEach
    void setUp() throws Exception {
        SdkHttpClient httpClient = mock(SdkHttpClient.class);
        ExecutableHttpRequest call = mock(ExecutableHttpRequest.class);
        when(httpClient.prepareRequest(any())).thenReturn(call);
        when(httpClient.clientName()).thenReturn("UNKNOWN");
        SdkHttpResponse response = SdkHttpResponse.builder().statusCode(200).putHeader("x-test", "original").build();
        when(call.call()).thenReturn(HttpExecuteResponse.builder().response(response).responseBody(body("original")).build());
        client = HttpTestUtils.testClientBuilder().httpClient(httpClient).build();
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void execute_whenInterceptorModifiesResponse_afterTransmissionSeesOriginalAndHandlerSeesModified() {
        RecordingInterceptor interceptor = new RecordingInterceptor(true);

        Pair<SdkHttpResponse, String> handled = execute(interceptor);

        assertThat(interceptor.calls).containsExactly("afterTransmission", "modifyHttpResponse", "beforeUnmarshalling");
        assertThat(interceptor.afterTransmissionResponse.firstMatchingHeader("x-test")).hasValue("original");
        assertThat(interceptor.beforeUnmarshallingResponse.firstMatchingHeader("x-test")).hasValue("modified");
        assertThat(handled.left().firstMatchingHeader("x-test")).hasValue("modified");
        assertThat(handled.right()).isEqualTo("modified");
    }

    @Test
    void execute_whenInterceptorDoesNotModifyResponse_handlerSeesOriginal() {
        Pair<SdkHttpResponse, String> handled = execute(new RecordingInterceptor(false));

        assertThat(handled.left().firstMatchingHeader("x-test")).hasValue("original");
        assertThat(handled.right()).isEqualTo("original");
    }

    private Pair<SdkHttpResponse, String> execute(ExecutionInterceptor interceptor) {
        SdkHttpFullRequest request = ValidSdkObjects.sdkHttpFullRequest().build();
        ExecutionContext executionContext =
            ExecutionContext.builder()
                            .signer(new NoOpSigner())
                            .interceptorChain(new ExecutionInterceptorChain(Collections.singletonList(interceptor)))
                            .executionAttributes(new ExecutionAttributes())
                            .interceptorContext(InterceptorContext.builder()
                                                                  .request(NoopTestRequest.builder().build())
                                                                  .httpRequest(request)
                                                                  .build())
                            .metricCollector(MetricCollector.create("ApiCall"))
                            .build();

        return client.requestExecutionBuilder()
                     .request(request)
                     .originalRequest(NoopTestRequest.builder().build())
                     .executionContext(executionContext)
                     .execute(combinedSyncResponseHandler(
                         (response, executionAttributes) ->
                             Pair.of((SdkHttpResponse) response, IoUtils.toUtf8String(response.content().get())),
                         null));
    }

    private static AbortableInputStream body(String content) {
        return AbortableInputStream.create(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static final class RecordingInterceptor implements ExecutionInterceptor {
        private final boolean modify;
        private final List<String> calls = new ArrayList<>();
        private SdkHttpResponse afterTransmissionResponse;
        private SdkHttpResponse beforeUnmarshallingResponse;

        private RecordingInterceptor(boolean modify) {
            this.modify = modify;
        }

        @Override
        public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
            calls.add("afterTransmission");
            afterTransmissionResponse = context.httpResponse();
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context,
                                                  ExecutionAttributes executionAttributes) {
            calls.add("modifyHttpResponse");
            return modify ? context.httpResponse().toBuilder().putHeader("x-test", "modified").build()
                          : context.httpResponse();
        }

        @Override
        public Optional<InputStream> modifyHttpResponseContent(Context.ModifyHttpResponse context,
                                                               ExecutionAttributes executionAttributes) {
            return modify ? Optional.of(body("modified")) : context.responseBody();
        }

        @Override
        public void beforeUnmarshalling(Context.BeforeUnmarshalling context, ExecutionAttributes executionAttributes) {
            calls.add("beforeUnmarshalling");
            beforeUnmarshallingResponse = context.httpResponse();
        }
    }
}
