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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Pair;
import utils.ValidSdkObjects;

class AwsChunkedResponseDecodingStageTest {

    private static final String ENCODED = "5\r\nhello\r\n6\r\n world\r\n0\r\nx-amz-checksum-crc32:abc==\r\n\r\n";

    private final AwsChunkedResponseDecodingStage stage = new AwsChunkedResponseDecodingStage();
    private final ExecutionAttributes attributes = new ExecutionAttributes();
    private final RequestExecutionContext context = context(attributes);

    @Test
    void execute_whenAwsChunkedResponse_decodesBodyRewritesHeadersAndExposesTrailers() throws Exception {
        attributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_DECODING, true);

        SdkHttpFullResponse result = execute(awsChunkedResponse().content(body(ENCODED)).build());

        assertThat(result.firstMatchingHeader("Content-Encoding")).isEmpty();
        assertThat(result.firstMatchingHeader("Content-Length")).hasValue("11");
        assertThat(result.firstMatchingHeader("x-amz-decoded-content-length")).isEmpty();
        assertThat(result.firstMatchingHeader("x-amz-trailer")).isEmpty();
        assertThat(context.executionContext().interceptorContext().httpResponse()).isSameAs(result);
        assertThat(context.executionContext().interceptorContext().responseBody()).containsSame(result.content().get());

        assertThat(IoUtils.toUtf8String(result.content().get())).isEqualTo("hello world");
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_TRAILERS).get())
            .containsEntry("x-amz-checksum-crc32", "abc==");
    }

    @Test
    void execute_whenDecodingNotEnabled_returnsInputUnchanged() throws Exception {
        Pair<SdkHttpFullRequest, SdkHttpFullResponse> input =
            Pair.of(ValidSdkObjects.sdkHttpFullRequest().build(), awsChunkedResponse().content(body(ENCODED)).build());

        assertThat(stage.execute(input, context)).isSameAs(input);
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_TRAILERS)).isNull();
    }

    @Test
    void execute_whenAwsChunkedResponseHasNoBody_onlyRewritesHeaders() throws Exception {
        attributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_DECODING, true);

        SdkHttpFullResponse result = execute(awsChunkedResponse().build());

        assertThat(result.firstMatchingHeader("Content-Encoding")).isEmpty();
        assertThat(result.content()).isEmpty();
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_TRAILERS)).isNull();
    }

    @Test
    void execute_whenDecodedContentLengthMissing_throwsAndAbortsBody() {
        attributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_DECODING, true);
        AtomicBoolean aborted = new AtomicBoolean();
        SdkHttpFullResponse response = awsChunkedResponse().removeHeader("x-amz-decoded-content-length")
                                                           .content(trackedBody(ENCODED, aborted))
                                                           .build();

        assertThatThrownBy(() -> execute(response))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("x-amz-decoded-content-length header is missing");
        assertThat(aborted).isTrue();
    }

    @Test
    void execute_whenAwsChunkedIsNotLastEncoding_throwsAndAbortsBody() {
        attributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_DECODING, true);
        AtomicBoolean aborted = new AtomicBoolean();
        SdkHttpFullResponse response = awsChunkedResponse().putHeader("Content-Encoding", "aws-chunked,gzip")
                                                           .content(trackedBody(ENCODED, aborted))
                                                           .build();

        assertThatThrownBy(() -> execute(response)).isInstanceOf(SdkClientException.class);
        assertThat(aborted).isTrue();
    }

    @Test
    void execute_whenEarlierAttemptLeftTrailers_clearsThem() throws Exception {
        attributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_TRAILERS, Collections::emptyMap);

        execute(SdkHttpFullResponse.builder().statusCode(200).content(body("plain")).build());

        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_TRAILERS)).isNull();
    }

    private SdkHttpFullResponse execute(SdkHttpFullResponse response) throws Exception {
        return stage.execute(Pair.of(ValidSdkObjects.sdkHttpFullRequest().build(), response), context).right();
    }

    private static RequestExecutionContext context(ExecutionAttributes attributes) {
        ExecutionContext executionContext =
            ExecutionContext.builder()
                            .interceptorContext(InterceptorContext.builder().request(ValidSdkObjects.sdkRequest()).build())
                            .executionAttributes(attributes)
                            .build();
        return RequestExecutionContext.builder()
                                      .originalRequest(ValidSdkObjects.sdkRequest())
                                      .executionContext(executionContext)
                                      .build();
    }

    private static SdkHttpFullResponse.Builder awsChunkedResponse() {
        return SdkHttpFullResponse.builder()
                                  .statusCode(200)
                                  .putHeader("Content-Encoding", "aws-chunked")
                                  .putHeader("x-amz-decoded-content-length", "11")
                                  .putHeader("x-amz-trailer", "x-amz-checksum-crc32");
    }

    private static AbortableInputStream body(String content) {
        return AbortableInputStream.create(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static AbortableInputStream trackedBody(String content, AtomicBoolean aborted) {
        return AbortableInputStream.create(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                                           () -> aborted.set(true));
    }
}
