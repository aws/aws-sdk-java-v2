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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.IoUtils;
import utils.HttpTestUtils;
import utils.ValidSdkObjects;

/**
 * Runs a request through the full sync pipeline to verify where aws-chunked decoding happens relative to interceptors.
 */
class AmazonSyncHttpClientAwsChunkedDecodingTest {

    private static final String ENCODED = "5\r\nhello\r\n6\r\n world\r\n0\r\nx-amz-checksum-crc32:abc==\r\n\r\n";

    private final RecordingInterceptor interceptor = new RecordingInterceptor();
    private final ExecutionAttributes attributes = new ExecutionAttributes();
    private AmazonSyncHttpClient client;

    @BeforeEach
    void setUp() throws Exception {
        SdkHttpClient httpClient = mock(SdkHttpClient.class);
        ExecutableHttpRequest call = mock(ExecutableHttpRequest.class);
        when(httpClient.prepareRequest(any())).thenReturn(call);
        when(httpClient.clientName()).thenReturn("UNKNOWN");
        when(call.call()).thenReturn(awsChunkedResponse());
        client = HttpTestUtils.testClientBuilder().httpClient(httpClient).build();
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void execute_whenDecodingEnabled_afterTransmissionSeesRawAndModifyHttpResponseSeesDecoded() {
        attributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_DECODING, true);

        String body = execute();

        assertThat(interceptor.afterTransmissionResponse.firstMatchingHeader("Content-Encoding")).hasValue("aws-chunked");
        assertThat(interceptor.modifyHttpResponseResponse.firstMatchingHeader("Content-Encoding")).isEmpty();
        assertThat(interceptor.modifyHttpResponseResponse.firstMatchingHeader("Content-Length")).hasValue("11");
        assertThat(body).isEqualTo("hello world");
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_TRAILERS).get())
            .containsEntry("x-amz-checksum-crc32", "abc==");
    }

    @Test
    void execute_whenDecodingNotEnabled_bodyIsUnchanged() {
        String body = execute();

        assertThat(interceptor.modifyHttpResponseResponse.firstMatchingHeader("Content-Encoding")).hasValue("aws-chunked");
        assertThat(body).isEqualTo(ENCODED);
    }

    private String execute() {
        SdkHttpFullRequest request = ValidSdkObjects.sdkHttpFullRequest().build();
        ExecutionContext executionContext =
            ExecutionContext.builder()
                            .signer(new NoOpSigner())
                            .interceptorChain(new ExecutionInterceptorChain(Collections.singletonList(interceptor)))
                            .executionAttributes(attributes)
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
                         (response, executionAttributes) -> IoUtils.toUtf8String(response.content().get()), null));
    }

    private static HttpExecuteResponse awsChunkedResponse() {
        SdkHttpResponse response = SdkHttpResponse.builder()
                                                  .statusCode(200)
                                                  .putHeader("Content-Encoding", "aws-chunked")
                                                  .putHeader("x-amz-decoded-content-length", "11")
                                                  .putHeader("x-amz-trailer", "x-amz-checksum-crc32")
                                                  .build();
        AbortableInputStream body =
            AbortableInputStream.create(new ByteArrayInputStream(ENCODED.getBytes(StandardCharsets.UTF_8)));
        return HttpExecuteResponse.builder().response(response).responseBody(body).build();
    }

    private static final class RecordingInterceptor implements ExecutionInterceptor {
        private SdkHttpResponse afterTransmissionResponse;
        private SdkHttpResponse modifyHttpResponseResponse;

        @Override
        public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
            afterTransmissionResponse = context.httpResponse();
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context,
                                                  ExecutionAttributes executionAttributes) {
            modifyHttpResponseResponse = context.httpResponse();
            return context.httpResponse();
        }
    }
}
