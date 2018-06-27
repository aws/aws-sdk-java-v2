/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.protocol.tests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.MembersInHeadersRequest;
import software.amazon.awssdk.services.protocolrestjson.model.MembersInHeadersResponse;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;

/**
 * Verify that request handler hooks are behaving as expected.
 */
public class ExecutionInterceptorTest {
    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().port(0), false);

    private static final String MEMBERS_IN_HEADERS_PATH = "/2016-03-11/membersInHeaders";
    private static final String STREAMING_INPUT_PATH = "/2016-03-11/streamingInputOperation";
    private static final String STREAMING_OUTPUT_PATH = "/2016-03-11/streamingOutputOperation";

    @Test
    public void sync_success_allInterceptorMethodsCalled() {
        // Given
        ExecutionInterceptor interceptor = mock(MessageUpdatingInterceptor.class, CALLS_REAL_METHODS);
        ProtocolRestJsonClient client = client(interceptor);
        MembersInHeadersRequest request = MembersInHeadersRequest.builder().build();
        stubFor(post(urlPathEqualTo(MEMBERS_IN_HEADERS_PATH)).willReturn(aResponse().withStatus(200).withBody("")));

        // When
        MembersInHeadersResponse result = client.membersInHeaders(request);

        // Expect
        expectAllMethodsCalled(interceptor, request, null);
        validateRequestResponse(result);
    }

    @Test
    public void async_success_allInterceptorMethodsCalled()
            throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        ExecutionInterceptor interceptor = mock(MessageUpdatingInterceptor.class, CALLS_REAL_METHODS);
        ProtocolRestJsonAsyncClient client = asyncClient(interceptor);
        MembersInHeadersRequest request = MembersInHeadersRequest.builder().build();
        stubFor(post(urlPathEqualTo(MEMBERS_IN_HEADERS_PATH)).willReturn(aResponse().withStatus(200).withBody("")));

        // When
        MembersInHeadersResponse result = client.membersInHeaders(request).get(10, TimeUnit.SECONDS);

        // Expect
        expectAllMethodsCalled(interceptor, request, null);
        validateRequestResponse(result);
    }

    @Test
    public void sync_streamingInput_success_allInterceptorMethodsCalled() throws IOException {
        // Given
        ExecutionInterceptor interceptor = mock(NoOpInterceptor.class, CALLS_REAL_METHODS);
        ProtocolRestJsonClient client = client(interceptor);
        StreamingInputOperationRequest request = StreamingInputOperationRequest.builder().build();
        stubFor(post(urlPathEqualTo(STREAMING_INPUT_PATH)).willReturn(aResponse().withStatus(200).withBody("")));

        // When
        client.streamingInputOperation(request, RequestBody.fromBytes(new byte[] {0}));

        // Expect
        Context.BeforeTransmission beforeTransmissionArg = captureBeforeTransmissionArg(interceptor);
        beforeTransmissionArg.httpRequest().content().get().reset();
        assertThat(beforeTransmissionArg.httpRequest().content().get().read()).isEqualTo(0);
    }

    @Test
    public void async_streamingInput_success_allInterceptorMethodsCalled()
            throws ExecutionException, InterruptedException, TimeoutException, IOException {
        // Given
        ExecutionInterceptor interceptor = mock(NoOpInterceptor.class, CALLS_REAL_METHODS);
        ProtocolRestJsonAsyncClient client = asyncClient(interceptor);
        StreamingInputOperationRequest request = StreamingInputOperationRequest.builder().build();
        stubFor(post(urlPathEqualTo(STREAMING_INPUT_PATH)).willReturn(aResponse().withStatus(200).withBody("")));

        // When
        client.streamingInputOperation(request, new NoOpAsyncRequestBody()).get(10, TimeUnit.SECONDS);

        // Expect
        Context.BeforeTransmission beforeTransmissionArg = captureBeforeTransmissionArg(interceptor);
        beforeTransmissionArg.httpRequest().content().get().reset();

        // TODO: The content should actually be empty to match responses. We can fix this by updating the StructuredJsonGenerator
        // to use null for NO-OP marshalling of payloads. This will break streaming POST operations for JSON because of a hack in
        // the MoveParametersToBodyStage, but we can move the logic from there into the query marshallers (why the hack exists)
        // and then everything should be good for JSON.
        assertThat(beforeTransmissionArg.httpRequest().content().get().read()).isEqualTo(-1);
    }

    @Test
    public void sync_streamingOutput_success_allInterceptorMethodsCalled() throws IOException {
        // Given
        ExecutionInterceptor interceptor = mock(NoOpInterceptor.class, CALLS_REAL_METHODS);
        ProtocolRestJsonClient client = client(interceptor);
        StreamingOutputOperationRequest request = StreamingOutputOperationRequest.builder().build();
        stubFor(post(urlPathEqualTo(STREAMING_OUTPUT_PATH)).willReturn(aResponse().withStatus(200).withBody("\0")));

        // When
        client.streamingOutputOperation(request, (r, i) -> {
            assertThat(i.read()).isEqualTo(0);
            // TODO: We have to return "r" here. We should verify other response types are cool once we switch this off of
            // being the unmarshaller
            return r;
        });

        // Expect
        Context.AfterTransmission afterTransmissionArg = captureAfterTransmissionArg(interceptor);
        // TODO: When we don't always close the input stream, make sure we can read the service's '0' response.
        assertThat(afterTransmissionArg.httpResponse().content()).isPresent();
    }

    @Test
    public void async_streamingOutput_success_allInterceptorMethodsCalled()
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        // Given
        ExecutionInterceptor interceptor = mock(NoOpInterceptor.class, CALLS_REAL_METHODS);
        ProtocolRestJsonAsyncClient client = asyncClient(interceptor);
        StreamingOutputOperationRequest request = StreamingOutputOperationRequest.builder().build();
        stubFor(post(urlPathEqualTo(STREAMING_OUTPUT_PATH)).willReturn(aResponse().withStatus(200).withBody("\0")));

        // When
        client.streamingOutputOperation(request, new NoOpAsyncResponseTransformer()).get(10, TimeUnit.SECONDS);

        // Expect
        Context.AfterTransmission afterTransmissionArg = captureAfterTransmissionArg(interceptor);
        assertThat(afterTransmissionArg.httpResponse().content()).isNotPresent();
    }

    @Test
    public void sync_serviceException_failureInterceptorMethodsCalled() {
        // Given
        ExecutionInterceptor interceptor = mock(MessageUpdatingInterceptor.class, CALLS_REAL_METHODS);
        ProtocolRestJsonClient client = client(interceptor);
        MembersInHeadersRequest request = MembersInHeadersRequest.builder().build();

        // When
        assertThatExceptionOfType(SdkServiceException.class).isThrownBy(() -> client.membersInHeaders(request));

        // Expect
        expectServiceCallErrorMethodsCalled(interceptor);
    }

    @Test
    public void async_serviceException_failureInterceptorMethodsCalled() throws ExecutionException, InterruptedException {
        // Given
        ExecutionInterceptor interceptor = mock(MessageUpdatingInterceptor.class, CALLS_REAL_METHODS);
        ProtocolRestJsonAsyncClient client = asyncClient(interceptor);
        MembersInHeadersRequest request = MembersInHeadersRequest.builder().build();

        // When
        assertThatExceptionOfType(ExecutionException.class).isThrownBy(() -> client.membersInHeaders(request).get())
                                                           .withCauseInstanceOf(SdkServiceException.class);

        // Expect
        expectServiceCallErrorMethodsCalled(interceptor);
    }

    @Test
    public void sync_interceptorException_failureInterceptorMethodsCalled() {
        // Given
        ExecutionInterceptor interceptor = mock(MessageUpdatingInterceptor.class, CALLS_REAL_METHODS);
        RuntimeException exception = new RuntimeException("Uh oh!");
        doThrow(exception).when(interceptor).afterExecution(any(), any());

        ProtocolRestJsonClient client = client(interceptor);
        MembersInHeadersRequest request = MembersInHeadersRequest.builder().build();
        stubFor(post(urlPathEqualTo(MEMBERS_IN_HEADERS_PATH)).willReturn(aResponse().withStatus(200).withBody("")));

        // When
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> client.membersInHeaders(request));

        // Expect
        expectAllMethodsCalled(interceptor, request, exception);
    }

    @Test
    public void async_interceptorException_failureInterceptorMethodsCalled() {
        // Given
        ExecutionInterceptor interceptor = mock(MessageUpdatingInterceptor.class, CALLS_REAL_METHODS);
        RuntimeException exception = new RuntimeException("Uh oh!");
        doThrow(exception).when(interceptor).afterExecution(any(), any());

        ProtocolRestJsonAsyncClient client = asyncClient(interceptor);
        MembersInHeadersRequest request = MembersInHeadersRequest.builder().build();
        stubFor(post(urlPathEqualTo(MEMBERS_IN_HEADERS_PATH)).willReturn(aResponse().withStatus(200).withBody("")));

        // When
        assertThatExceptionOfType(ExecutionException.class).isThrownBy(() -> client.membersInHeaders(request).get())
                                                           .withCause(exception);

        // Expect
        expectAllMethodsCalled(interceptor, request, exception);
    }

    private Context.BeforeTransmission captureBeforeTransmissionArg(ExecutionInterceptor interceptor) {
        ArgumentCaptor<Context.BeforeTransmission> beforeTransmissionArg = ArgumentCaptor.forClass(Context.BeforeTransmission.class);

        InOrder inOrder = Mockito.inOrder(interceptor);
        inOrder.verify(interceptor).beforeExecution(any(), any());
        inOrder.verify(interceptor).modifyRequest(any(), any());
        inOrder.verify(interceptor).beforeMarshalling(any(), any());
        inOrder.verify(interceptor).afterMarshalling(any(), any());
        inOrder.verify(interceptor).modifyHttpRequest(any(), any());
        inOrder.verify(interceptor).beforeTransmission(beforeTransmissionArg.capture(), any());
        inOrder.verify(interceptor).afterTransmission(any(), any());
        inOrder.verify(interceptor).modifyHttpResponse(any(), any());
        inOrder.verify(interceptor).beforeUnmarshalling(any(), any());
        inOrder.verify(interceptor).afterUnmarshalling(any(), any());
        inOrder.verify(interceptor).modifyResponse(any(), any());
        inOrder.verify(interceptor).afterExecution(any(), any());
        verifyNoMoreInteractions(interceptor);
        return beforeTransmissionArg.getValue();
    }

    private Context.AfterTransmission captureAfterTransmissionArg(ExecutionInterceptor interceptor) {
        ArgumentCaptor<Context.AfterTransmission> afterTransmissionArg = ArgumentCaptor.forClass(Context.AfterTransmission.class);

        InOrder inOrder = Mockito.inOrder(interceptor);
        inOrder.verify(interceptor).beforeExecution(any(), any());
        inOrder.verify(interceptor).modifyRequest(any(), any());
        inOrder.verify(interceptor).beforeMarshalling(any(), any());
        inOrder.verify(interceptor).afterMarshalling(any(), any());
        inOrder.verify(interceptor).modifyHttpRequest(any(), any());
        inOrder.verify(interceptor).beforeTransmission(any(), any());
        inOrder.verify(interceptor).afterTransmission(afterTransmissionArg.capture(), any());
        inOrder.verify(interceptor).modifyHttpResponse(any(), any());
        inOrder.verify(interceptor).beforeUnmarshalling(any(), any());
        inOrder.verify(interceptor).afterUnmarshalling(any(), any());
        inOrder.verify(interceptor).modifyResponse(any(), any());
        inOrder.verify(interceptor).afterExecution(any(), any());
        verifyNoMoreInteractions(interceptor);
        return afterTransmissionArg.getValue();
    }

    private void expectAllMethodsCalled(ExecutionInterceptor interceptor, SdkRequest inputRequest, Exception expectedException) {
        ArgumentCaptor<ExecutionAttributes> attributes = ArgumentCaptor.forClass(ExecutionAttributes.class);

        ArgumentCaptor<Context.BeforeExecution> beforeExecutionArg = ArgumentCaptor.forClass(Context.BeforeExecution.class);
        ArgumentCaptor<Context.BeforeMarshalling> modifyRequestArg = ArgumentCaptor.forClass(Context.BeforeMarshalling.class);
        ArgumentCaptor<Context.BeforeMarshalling> beforeMarshallingArg = ArgumentCaptor.forClass(Context.BeforeMarshalling.class);
        ArgumentCaptor<Context.AfterMarshalling> afterMarshallingArg = ArgumentCaptor.forClass(Context.AfterMarshalling.class);
        ArgumentCaptor<Context.BeforeTransmission> modifyHttpRequestArg = ArgumentCaptor.forClass(Context.BeforeTransmission.class);
        ArgumentCaptor<Context.BeforeTransmission> beforeTransmissionArg = ArgumentCaptor.forClass(Context.BeforeTransmission.class);
        ArgumentCaptor<Context.AfterTransmission> afterTransmissionArg = ArgumentCaptor.forClass(Context.AfterTransmission.class);
        ArgumentCaptor<Context.BeforeUnmarshalling> modifyHttpResponseArg = ArgumentCaptor.forClass(Context.BeforeUnmarshalling.class);
        ArgumentCaptor<Context.BeforeUnmarshalling> beforeUnmarshallingArg = ArgumentCaptor.forClass(Context.BeforeUnmarshalling.class);
        ArgumentCaptor<Context.AfterUnmarshalling> afterUnmarshallingArg = ArgumentCaptor.forClass(Context.AfterUnmarshalling.class);
        ArgumentCaptor<Context.AfterExecution> modifyResponseArg = ArgumentCaptor.forClass(Context.AfterExecution.class);
        ArgumentCaptor<Context.AfterExecution> afterExecutionArg = ArgumentCaptor.forClass(Context.AfterExecution.class);

        // Verify methods are called in the right order
        InOrder inOrder = Mockito.inOrder(interceptor);
        inOrder.verify(interceptor).beforeExecution(beforeExecutionArg.capture(), attributes.capture());
        inOrder.verify(interceptor).modifyRequest(modifyRequestArg.capture(), attributes.capture());
        inOrder.verify(interceptor).beforeMarshalling(beforeMarshallingArg.capture(), attributes.capture());
        inOrder.verify(interceptor).afterMarshalling(afterMarshallingArg.capture(), attributes.capture());
        inOrder.verify(interceptor).modifyHttpRequest(modifyHttpRequestArg.capture(), attributes.capture());
        inOrder.verify(interceptor).beforeTransmission(beforeTransmissionArg.capture(), attributes.capture());
        inOrder.verify(interceptor).afterTransmission(afterTransmissionArg.capture(), attributes.capture());
        inOrder.verify(interceptor).modifyHttpResponse(modifyHttpResponseArg.capture(), attributes.capture());
        inOrder.verify(interceptor).beforeUnmarshalling(beforeUnmarshallingArg.capture(), attributes.capture());
        inOrder.verify(interceptor).afterUnmarshalling(afterUnmarshallingArg.capture(), attributes.capture());
        inOrder.verify(interceptor).modifyResponse(modifyResponseArg.capture(), attributes.capture());
        inOrder.verify(interceptor).afterExecution(afterExecutionArg.capture(), attributes.capture());
        if (expectedException != null) {
            ArgumentCaptor<Context.FailedExecution> failedExecutionArg = ArgumentCaptor.forClass(Context.FailedExecution.class);
            inOrder.verify(interceptor).onExecutionFailure(failedExecutionArg.capture(), attributes.capture());
            verifyFailedExecutionMethodCalled(failedExecutionArg, true);
            assertThat(failedExecutionArg.getValue().exception()).isEqualTo(expectedException);
        }
        verifyNoMoreInteractions(interceptor);

        // Verify beforeExecution gets untouched request
        assertThat(beforeExecutionArg.getValue().request()).isSameAs(inputRequest);

        // Verify methods were given correct parameters
        validateArgs(beforeExecutionArg.getValue(), null);
        validateArgs(modifyRequestArg.getValue(), null);
        validateArgs(beforeMarshallingArg.getValue(), "1");
        validateArgs(afterMarshallingArg.getValue(), "1", null);
        validateArgs(modifyHttpRequestArg.getValue(), "1", null);
        validateArgs(beforeTransmissionArg.getValue(), "1", "2");
        validateArgs(afterTransmissionArg.getValue(), "1", "2", null);
        validateArgs(modifyHttpResponseArg.getValue(), "1", "2", null);
        validateArgs(beforeUnmarshallingArg.getValue(), "1", "2", "3");
        validateArgs(afterUnmarshallingArg.getValue(), "1", "2", "3", null);
        validateArgs(modifyResponseArg.getValue(), "1", "2", "3", null);
        validateArgs(afterExecutionArg.getValue(), "1", "2", "3", "4");

        // Verify same execution attributes were used for all method calls
        assertThat(attributes.getAllValues()).containsOnly(attributes.getAllValues().get(0));
    }

    private void validateRequestResponse(MembersInHeadersResponse outputResponse) {
        verify(postRequestedFor(urlPathEqualTo(MEMBERS_IN_HEADERS_PATH))
                       .withHeader("x-amz-string", equalTo("1"))
                       .withHeader("x-amz-integer", equalTo("2")));

        assertThat(outputResponse.integerMember()).isEqualTo(3);
        assertThat(outputResponse.stringMember()).isEqualTo("4");
    }

    private void expectServiceCallErrorMethodsCalled(ExecutionInterceptor interceptor) {
        ArgumentCaptor<ExecutionAttributes> attributes = ArgumentCaptor.forClass(ExecutionAttributes.class);
        ArgumentCaptor<Context.BeforeUnmarshalling> beforeUnmarshallingArg = ArgumentCaptor.forClass(Context.BeforeUnmarshalling.class);
        ArgumentCaptor<Context.FailedExecution> failedExecutionArg = ArgumentCaptor.forClass(Context.FailedExecution.class);

        InOrder inOrder = Mockito.inOrder(interceptor);
        inOrder.verify(interceptor).beforeExecution(any(), attributes.capture());
        inOrder.verify(interceptor).modifyRequest(any(), attributes.capture());
        inOrder.verify(interceptor).beforeMarshalling(any(), attributes.capture());
        inOrder.verify(interceptor).afterMarshalling(any(), attributes.capture());
        inOrder.verify(interceptor).modifyHttpRequest(any(), attributes.capture());
        inOrder.verify(interceptor).beforeTransmission(any(), attributes.capture());
        inOrder.verify(interceptor).afterTransmission(any(), attributes.capture());
        inOrder.verify(interceptor).modifyHttpResponse(any(), attributes.capture());
        inOrder.verify(interceptor).beforeUnmarshalling(beforeUnmarshallingArg.capture(), attributes.capture());
        inOrder.verify(interceptor).onExecutionFailure(failedExecutionArg.capture(), attributes.capture());
        verifyNoMoreInteractions(interceptor);

        // Verify same execution attributes were used for all method calls
        assertThat(attributes.getAllValues()).containsOnly(attributes.getAllValues().get(0));

        // Verify HTTP response
        assertThat(beforeUnmarshallingArg.getValue().httpResponse().statusCode()).isEqualTo(404);

        // Verify failed execution parameters
        assertThat(failedExecutionArg.getValue().exception()).isInstanceOf(SdkServiceException.class);
        verifyFailedExecutionMethodCalled(failedExecutionArg, false);
    }

    private void verifyFailedExecutionMethodCalled(ArgumentCaptor<Context.FailedExecution> failedExecutionArg,
                                                   boolean expectResponse) {
        MembersInHeadersRequest failedRequest = (MembersInHeadersRequest) failedExecutionArg.getValue().request();

        assertThat(failedRequest.stringMember()).isEqualTo("1");
        assertThat(failedExecutionArg.getValue().httpRequest()).hasValueSatisfying(httpRequest -> {
            assertThat(httpRequest.firstMatchingHeader("x-amz-string")).hasValue("1");
            assertThat(httpRequest.firstMatchingHeader("x-amz-integer")).hasValue("2");
        });
        assertThat(failedExecutionArg.getValue().httpResponse()).hasValueSatisfying(httpResponse -> {
            assertThat(httpResponse.firstMatchingHeader("x-amz-integer")).hasValue("3");
        });

        if (expectResponse) {
            assertThat(failedExecutionArg.getValue().response().map(MembersInHeadersResponse.class::cast)).hasValueSatisfying(response -> {
                assertThat(response.integerMember()).isEqualTo(3);
                assertThat(response.stringMember()).isEqualTo("4");
            });
        } else {
            assertThat(failedExecutionArg.getValue().response()).isNotPresent();
        }
    }

    private void validateArgs(Context.BeforeExecution context,
                              String expectedStringMemberValue) {
        MembersInHeadersRequest request = (MembersInHeadersRequest) context.request();
        assertThat(request.stringMember()).isEqualTo(expectedStringMemberValue);
    }

    private void validateArgs(Context.AfterMarshalling context,
                              String expectedStringMemberValue, String expectedIntegerHeaderValue) {
        validateArgs(context, expectedStringMemberValue);
        assertThat(context.httpRequest().firstMatchingHeader("x-amz-integer"))
                .isEqualTo(Optional.ofNullable(expectedIntegerHeaderValue));
        assertThat(context.httpRequest().firstMatchingHeader("x-amz-string"))
                .isEqualTo(Optional.ofNullable(expectedStringMemberValue));
    }

    private void validateArgs(Context.AfterTransmission context,
                              String expectedStringMemberValue, String expectedIntegerHeaderValue,
                              String expectedResponseIntegerHeaderValue) {
        validateArgs(context, expectedStringMemberValue, expectedIntegerHeaderValue);
        assertThat(context.httpResponse().firstMatchingHeader("x-amz-integer"))
                .isEqualTo(Optional.ofNullable(expectedResponseIntegerHeaderValue));
    }

    private void validateArgs(Context.AfterUnmarshalling context,
                              String expectedStringMemberValue, String expectedIntegerHeaderValue,
                              String expectedResponseIntegerHeaderValue, String expectedResponseStringMemberValue) {
        validateArgs(context, expectedStringMemberValue, expectedIntegerHeaderValue, expectedResponseIntegerHeaderValue);
        MembersInHeadersResponse response = (MembersInHeadersResponse) context.response();
        assertThat(response.integerMember()).isEqualTo(toInt(expectedResponseIntegerHeaderValue));
        assertThat(response.stringMember()).isEqualTo(expectedResponseStringMemberValue);
    }

    private Integer toInt(String stringInteger) {
        return stringInteger == null ? null : Integer.parseInt(stringInteger);
    }

    private ProtocolRestJsonClient client(ExecutionInterceptor interceptor) {
        return initializeAndBuild(ProtocolRestJsonClient.builder(), interceptor);
    }

    private ProtocolRestJsonAsyncClient asyncClient(ExecutionInterceptor interceptor) {
        return initializeAndBuild(ProtocolRestJsonAsyncClient.builder(), interceptor);
    }

    private <T extends AwsClientBuilder<?, U>, U> U initializeAndBuild(T builder, ExecutionInterceptor interceptor) {
        return builder.region(Region.US_WEST_1)
                      .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                      .credentialsProvider(StaticCredentialsProvider.create(AwsCredentials.create("akid", "skid")))
                      .overrideConfiguration(ClientOverrideConfiguration.builder()
                                                                        .addExecutionInterceptor(interceptor)
                                                                        .build())
                      .build();
    }

    private static class MessageUpdatingInterceptor implements ExecutionInterceptor {
        @Override
        public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
            MembersInHeadersRequest request = (MembersInHeadersRequest) context.request();
            return request.toBuilder()
                    .stringMember("1")
                    .build();
        }

        @Override
        public SdkHttpFullRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                                    ExecutionAttributes executionAttributes) {
            SdkHttpFullRequest httpRequest = context.httpRequest();
            return httpRequest.copy(b -> b.header("x-amz-integer", "2"));
        }

        @Override
        public SdkHttpFullResponse modifyHttpResponse(Context.ModifyHttpResponse context,
                                                      ExecutionAttributes executionAttributes) {
            SdkHttpFullResponse httpResponse = context.httpResponse();
            return httpResponse.copy(b -> b.header("x-amz-integer", Collections.singletonList("3")));
        }

        @Override
        public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
            MembersInHeadersResponse response = (MembersInHeadersResponse) context.response();
            return response.toBuilder()
                    .stringMember("4")
                    .build();
        }
    }

    private static class NoOpInterceptor implements ExecutionInterceptor {

    }

    private static class NoOpAsyncRequestBody implements AsyncRequestBody {
        @Override
        public long contentLength() {
            return 0;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            // TODO: For content-length 0 should we require them to do this?
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    s.onComplete();
                }

                @Override
                public void cancel() {
                }
            });
        }
    }

    private static class NoOpAsyncResponseTransformer
            implements AsyncResponseTransformer<StreamingOutputOperationResponse, Object> {
        private StreamingOutputOperationResponse response;

        @Override
        public void responseReceived(StreamingOutputOperationResponse response) {
            this.response = response;
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            publisher.subscribe(new Subscriber<ByteBuffer>() {
                private Subscription s;

                @Override
                public void onSubscribe(Subscription s) {
                    this.s = s;
                    s.request(1); // TODO: Can we simplify the implementation of a "ignore everything" response handler?
                }

                @Override
                public void onNext(ByteBuffer byteBuffer) {
                    s.request(1);
                }

                @Override
                public void onError(Throwable t) {

                }

                @Override
                public void onComplete() {

                }
            });
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            throwable.printStackTrace();
        }

        @Override
        public Object complete() {
            // TODO: If I throw an exception here, the future isn't completed exceptionally.
            // TODO: We have to return "response" here. We should verify other response types are cool once we switch this off of
            // being the unmarshaller
            return response;
        }
    }
}
