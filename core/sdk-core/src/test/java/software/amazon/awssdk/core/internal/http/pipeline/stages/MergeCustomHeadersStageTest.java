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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import utils.ValidSdkObjects;

public class MergeCustomHeadersStageTest {
    // List of headers that may appear only once in a request; i.e. is not a list of values.
    // Taken from https://github.com/apache/httpcomponents-client/blob/81c1bc4dc3ca5a3134c5c60e8beff08be2fd8792/httpclient5-cache/src/test/java/org/apache/hc/client5/http/impl/cache/HttpTestUtils.java#L69-L85
    // with modifications:
    // removed: accept-ranges, if-match, if-none-match, vary since it looks like they're defined as lists
    public static Stream<String> singleHeaders() {
        return Stream.of("age", "authorization", "content-length", "content-location", "content-md5", "content-range",
                         "content-type", "date", "etag", "expires", "from", "host", "if-modified-since", "if-range",
                         "if-unmodified-since", "last-modified", "location", "max-forwards", "proxy-authorization", "range",
                         "referer", "retry-after", "server", "user-agent");
    }

    @ParameterizedTest
    @MethodSource("singleHeaders")
    void singleHeader_inMarshalledRequest_overriddenOnClient(String singleHeaderName) throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = SdkHttpFullRequest.builder();

        RequestExecutionContext ctx = requestContext(NoopTestRequest.builder().build());
        requestBuilder.putHeader(singleHeaderName, "marshaller");

        Map<String, List<String>> clientHeaders = new HashMap<>();
        clientHeaders.put(singleHeaderName, Collections.singletonList("client"));

        HttpClientDependencies clientDeps = HttpClientDependencies.builder()
                .clientConfiguration(SdkClientConfiguration.builder()
                        .option(SdkClientOption.ADDITIONAL_HTTP_HEADERS, clientHeaders)
                        .build())
                .build();
        MergeCustomHeadersStage stage = new MergeCustomHeadersStage(clientDeps);

        stage.execute(requestBuilder, ctx);

        assertThat(requestBuilder.headers().get(singleHeaderName)).containsExactly("client");
    }

    @ParameterizedTest
    @MethodSource("singleHeaders")
    void singleHeader_inMarshalledRequest_overriddenOnRequest(String singleHeaderName) throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = SdkHttpFullRequest.builder();
        requestBuilder.putHeader(singleHeaderName, "marshaller");

        RequestExecutionContext ctx = requestContext(NoopTestRequest.builder()
                .overrideConfiguration(SdkRequestOverrideConfiguration.builder()
                        .putHeader(singleHeaderName, "request").build())
                .build());

        HttpClientDependencies clientDeps = HttpClientDependencies.builder()
                .clientConfiguration(SdkClientConfiguration.builder()
                        .option(SdkClientOption.ADDITIONAL_HTTP_HEADERS, Collections.emptyMap())
                        .build())
                .build();
        MergeCustomHeadersStage stage = new MergeCustomHeadersStage(clientDeps);

        stage.execute(requestBuilder, ctx);

        assertThat(requestBuilder.headers().get(singleHeaderName)).containsExactly("request");
    }

    @ParameterizedTest
    @MethodSource("singleHeaders")
    void singleHeader_inClient_overriddenOnRequest(String singleHeaderName) throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = SdkHttpFullRequest.builder();

        RequestExecutionContext ctx = requestContext(NoopTestRequest.builder()
                .overrideConfiguration(SdkRequestOverrideConfiguration.builder()
                        .putHeader(singleHeaderName, "request").build())
                .build());

        Map<String, List<String>> clientHeaders = new HashMap<>();
        clientHeaders.put(singleHeaderName, Collections.singletonList("client"));
        HttpClientDependencies clientDeps = HttpClientDependencies.builder()
                .clientConfiguration(SdkClientConfiguration.builder()
                        .option(SdkClientOption.ADDITIONAL_HTTP_HEADERS, clientHeaders)
                        .build())
                .build();
        MergeCustomHeadersStage stage = new MergeCustomHeadersStage(clientDeps);

        stage.execute(requestBuilder, ctx);

        assertThat(requestBuilder.headers().get(singleHeaderName)).containsExactly("request");
    }

    @ParameterizedTest
    @MethodSource("singleHeaders")
    void singleHeader_inMarshalledRequest_inClient_inRequest(String singleHeaderName) throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = SdkHttpFullRequest.builder();
        requestBuilder.putHeader(singleHeaderName, "marshaller");

        RequestExecutionContext ctx = requestContext(NoopTestRequest.builder()
                .overrideConfiguration(SdkRequestOverrideConfiguration.builder()
                        .putHeader(singleHeaderName, "request").build())
                .build());

        Map<String, List<String>> clientHeaders = new HashMap<>();
        clientHeaders.put(singleHeaderName, Collections.singletonList("client"));
        HttpClientDependencies clientDeps = HttpClientDependencies.builder()
                .clientConfiguration(SdkClientConfiguration.builder()
                        .option(SdkClientOption.ADDITIONAL_HTTP_HEADERS, clientHeaders)
                        .build())
                .build();
        MergeCustomHeadersStage stage = new MergeCustomHeadersStage(clientDeps);

        stage.execute(requestBuilder, ctx);

        assertThat(requestBuilder.headers().get(singleHeaderName)).containsExactly("request");
    }

    @ParameterizedTest
    @MethodSource("singleHeaders")
    void singleHeader_inRequestAsList_keepsMultipleValues(String singleHeaderName) throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = SdkHttpFullRequest.builder();
        requestBuilder.putHeader(singleHeaderName, "marshaller");

        RequestExecutionContext ctx = requestContext(NoopTestRequest.builder()
                .overrideConfiguration(SdkRequestOverrideConfiguration.builder()
                        .putHeader(singleHeaderName, Arrays.asList("request", "request2", "request3"))
                        .build())
                .build());

        Map<String, List<String>> clientHeaders = new HashMap<>();
        HttpClientDependencies clientDeps = HttpClientDependencies.builder()
                .clientConfiguration(SdkClientConfiguration.builder()
                        .option(SdkClientOption.ADDITIONAL_HTTP_HEADERS, clientHeaders)
                        .build())
                .build();
        MergeCustomHeadersStage stage = new MergeCustomHeadersStage(clientDeps);

        stage.execute(requestBuilder, ctx);

        assertThat(requestBuilder.headers().get(singleHeaderName)).containsExactly("request", "request2", "request3");
    }

    private RequestExecutionContext requestContext(SdkRequest request) {
        ExecutionContext executionContext = ClientExecutionAndRequestTimerTestUtils.executionContext(ValidSdkObjects.sdkHttpFullRequest().build());
        return RequestExecutionContext.builder()
                .executionContext(executionContext)
                .originalRequest(request)
                .build();
    }
}
