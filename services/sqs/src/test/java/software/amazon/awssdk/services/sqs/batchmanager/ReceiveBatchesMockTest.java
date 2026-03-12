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

package software.amazon.awssdk.services.sqs.batchmanager;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@ExtendWith(MockitoExtension.class)
class ReceiveBatchesMockTest {

    private static final int OFFSET_DELAY = 100;
    // Default queue attribute response with placeholders for parameters
    private static final String QUEUE_ATTRIBUTE_RESPONSE = "{\n" +
                                                           "  \"Attributes\": {\n" +
                                                           "    \"ReceiveMessageWaitTimeSeconds\": \"%s\",\n" +
                                                           "    \"VisibilityTimeout\": \"%s\"\n" +
                                                           "  }\n" +
                                                           "}";
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
                                                         .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
                                                         .configureStaticDsl(true)
                                                         .build();
    private SqsAsyncBatchManager receiveMessageBatchManager;

    @Test
    void testTimeoutOccursBeforeSqsResponds() throws Exception {
        setupBatchManager();

        // Delays for testing
        int queueAttributesApiDelay = 51;
        int receiveMessagesDelay = 150;

        // Stub the WireMock server to simulate delayed responses
        mockQueueAttributes(queueAttributesApiDelay);
        mockReceiveMessages(receiveMessagesDelay, 2);

        CompletableFuture<ReceiveMessageResponse> future = batchManagerReceiveMessage();
        assertThat(future.get(5, TimeUnit.SECONDS).messages()).isEmpty();

        Thread.sleep(queueAttributesApiDelay + receiveMessagesDelay + OFFSET_DELAY);

        CompletableFuture<ReceiveMessageResponse> secondCall = batchManagerReceiveMessage();
        assertThat(secondCall.get(5, TimeUnit.SECONDS).messages()).hasSize(2);
    }

    @Test
    void testResponseReceivedBeforeTimeout() throws Exception {
        setupBatchManager();

        // No delays set since we mock quick response from SQS.
        mockQueueAttributes();
        mockReceiveMessages(2);

        CompletableFuture<ReceiveMessageResponse> future = batchManagerReceiveMessage();
        assertThat(future.get(5, TimeUnit.SECONDS).messages()).hasSize(2);
    }

    @Test
    void testTimeoutOccursBasedOnUserSetWaitTime() throws Exception {
        setupBatchManager();

        // Delays for testing
        int queueAttributesApiDelay = 100;
        int receiveMessagesDelay = 100;

        // Configure response delays
        mockQueueAttributes(queueAttributesApiDelay);
        mockReceiveMessages(receiveMessagesDelay, 2);

        CompletableFuture<ReceiveMessageResponse> future = receiveMessageWithWaitTime(1);
        assertThat(future.get(5, TimeUnit.SECONDS).messages()).hasSize(2);
    }

    @Test
    void testMessagesAreFetchedFromBufferWhenAvailable() throws Exception {
        ApiCaptureInterceptor interceptor = new ApiCaptureInterceptor();
        SqsAsyncClient sqsAsyncClient = getAsyncClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
            .build();

        SqsAsyncBatchManager batchManager = sqsAsyncClient.batchManager();

        // Delays for testing
        int queueAttributesApiDelay = 100;
        int receiveMessagesDelay = 1000;

        // Setup delayed responses
        mockQueueAttributes(queueAttributesApiDelay);
        mockReceiveMessages(receiveMessagesDelay, 10);

        // First message should be empty due to delay
        CompletableFuture<ReceiveMessageResponse> firstMessage =
            batchManager.receiveMessage(r -> r.queueUrl("test").maxNumberOfMessages(1));
        assertThat(firstMessage.get(5, TimeUnit.SECONDS).messages()).isEmpty();

        // Wait for SQS message to be processed
        Thread.sleep(queueAttributesApiDelay + receiveMessagesDelay + OFFSET_DELAY);
        assertThat(interceptor.receiveApiCalls.get()).isEqualTo(1);
        assertThat(interceptor.getQueueAttributesApiCalls.get()).isEqualTo(1);
        interceptor.reset();

        // Fetch 10 messages from the buffer
        for (int i = 0; i < 10; i++) {
            CompletableFuture<ReceiveMessageResponse> future =
                batchManager.receiveMessage(r -> r.queueUrl("test").maxNumberOfMessages(1));
            ReceiveMessageResponse response = future.get(5, TimeUnit.SECONDS);
            assertThat(response.messages()).hasSize(1);
        }
        assertThat(interceptor.receiveApiCalls.get()).isEqualTo(0);
        assertThat(interceptor.getQueueAttributesApiCalls.get()).isEqualTo(0);
    }

    // Utility methods for reuse across tests

    private void setupBatchManager() {
        SqsAsyncClient sqsAsyncClient = getAsyncClientBuilder().build();
        receiveMessageBatchManager = sqsAsyncClient.batchManager();
    }

    private void mockQueueAttributes(int delay) {
        stubFor(post(urlEqualTo("/"))
                    .withHeader("x-amz-target", equalTo("AmazonSQS.GetQueueAttributes"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody(String.format(QUEUE_ATTRIBUTE_RESPONSE, "0", "30"))
                                    .withFixedDelay(delay)));
    }

    private void mockReceiveMessages(int delay, int numMessages) {
        stubFor(post(urlEqualTo("/"))
                    .withHeader("x-amz-target", equalTo("AmazonSQS.ReceiveMessage"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody(generateMessagesJson(numMessages))
                                    .withFixedDelay(delay)));
    }

    private void mockReceiveMessages(int numMessages) {
        stubFor(post(urlEqualTo("/"))
                    .withHeader("x-amz-target", equalTo("AmazonSQS.ReceiveMessage"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody(generateMessagesJson(numMessages))));
    }

    private void mockQueueAttributes( ) {
        stubFor(post(urlEqualTo("/"))
                    .withHeader("x-amz-target", equalTo("AmazonSQS.GetQueueAttributes"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody(String.format(QUEUE_ATTRIBUTE_RESPONSE, "0", "30"))));
    }

    private CompletableFuture<ReceiveMessageResponse> batchManagerReceiveMessage() {
        return receiveMessageBatchManager.receiveMessage(r -> r.queueUrl("test"));
    }

    private CompletableFuture<ReceiveMessageResponse> receiveMessageWithWaitTime(int waitTimeSeconds) {
        return receiveMessageBatchManager.receiveMessage(r -> r.queueUrl("test").waitTimeSeconds(waitTimeSeconds));
    }

    // Helper method for building the async client
    private SqsAsyncClientBuilder getAsyncClientBuilder() {
        return SqsAsyncClient.builder()
                             .endpointOverride(URI.create(String.format("http://localhost:%s/", wireMock.getPort())))
                             .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")));
    }

    // Utility to generate the response for multiple messages in JSON format
    private String generateMessagesJson(int numMessages) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"Messages\": [\n");
        for (int i = 0; i < numMessages; i++) {
            sb.append("    {\n");
            sb.append("      \"Body\": \"Message 6\",\n");
            sb.append("      \"MD5OfBody\": \"05d2a129ebdb00cfa6e92aaf9f090547\",\n");
            sb.append("      \"MessageId\": \"57d2\",\n");
            sb.append("      \"ReceiptHandle\": \"AQEB\"\n");
            sb.append("    }");
            if (i < numMessages - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }

    // Interceptor to capture the API call counts
    static class ApiCaptureInterceptor implements ExecutionInterceptor {

        AtomicInteger receiveApiCalls = new AtomicInteger();
        AtomicInteger getQueueAttributesApiCalls = new AtomicInteger();

        void reset() {
            receiveApiCalls.set(0);
            getQueueAttributesApiCalls.set(0);
        }

        @Override
        public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
            if (context.request() instanceof ReceiveMessageRequest) {
                receiveApiCalls.incrementAndGet();
            }
            if (context.request() instanceof GetQueueAttributesRequest) {
                getQueueAttributesApiCalls.incrementAndGet();
            }
        }
    }
}
