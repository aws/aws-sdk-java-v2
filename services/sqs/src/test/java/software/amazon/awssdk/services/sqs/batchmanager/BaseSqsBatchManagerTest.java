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
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Md5Utils;

public abstract class BaseSqsBatchManagerTest {

    protected static final String DEFAULT_QUEUE_URL = "SomeQueueUrl";
    private static final int DEFAULT_MAX_BATCH_OPEN = 200;


    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
                                                         .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
                                                         .configureStaticDsl(true)
                                                         .build();

    @Test
    public void sendMessageBatchFunction_batchMessageCorrectly() {
        String id1 = "0";
        String id2 = "1";
        String messageBody1 = getMd5Hash(id1);
        String messageBody2 = getMd5Hash(id2);
        String responseBody = String.format(
            "{\n" +
            "    \"Successful\": [\n" +
            "        {\n" +
            "            \"Id\": \"%s\",\n" +
            "            \"MD5OfMessageBody\": \"%s\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"Id\": \"%s\",\n" +
            "            \"MD5OfMessageBody\": \"%s\"\n" +
            "        }\n" +
            "    ]\n" +
            "}",
            id1, messageBody1, id2, messageBody2
        );

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(responseBody)));
        List<CompletableFuture<SendMessageResponse>> responses = createAndSendSendMessageRequests(id1, id2);

        SendMessageResponse completedResponse1 = responses.get(0).join();
        SendMessageResponse completedResponse2 = responses.get(1).join();
        assertThat(completedResponse1.md5OfMessageBody()).isEqualTo(messageBody1);
        assertThat(completedResponse2.md5OfMessageBody()).isEqualTo(messageBody2);
        verify(anyRequestedFor(anyUrl())
                   .withHeader("User-Agent", containing("hll/abm")));
    }

    @Test
    public void sendMessageBatchFunctionWithBatchEntryFailures_wrapFailureMessageInBatchEntry() {
        String id1 = "0";
        String id2 = "1";
        String errorCode = "400";
        String errorMessage = "Some error";
        String responseBody = String.format(
            "{\n" +
            "    \"Failed\": [\n" +
            "        {\n" +
            "            \"Id\": \"%s\",\n" +
            "            \"Code\": \"%s\",\n" +
            "            \"Message\": \"%s\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"Id\": \"%s\",\n" +
            "            \"Code\": \"%s\",\n" +
            "            \"Message\": \"%s\"\n" +
            "        }\n" +
            "    ]\n" +
            "}",
            id1, errorCode, errorMessage, id2, errorCode, errorMessage
        );

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(responseBody)));
        List<CompletableFuture<SendMessageResponse>> responses = createAndSendSendMessageRequests(id1, id2);

        CompletableFuture<SendMessageResponse> response1 = responses.get(0);
        CompletableFuture<SendMessageResponse> response2 = responses.get(1);
        assertThatThrownBy(() -> response1.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(SqsException.class).hasMessageContaining(errorMessage);
        assertThatThrownBy(() -> response2.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(SqsException.class).hasMessageContaining(errorMessage);
    }

    @Test
    public void sendMessageBatchFunctionReturnsWithError_completeMessagesExceptionally() {
        String id1 = "0";
        String id2 = "1";
        String responseBody = "{\n" +
                              "    \"__type\": \"com.amazonaws.sqs#QueueDoesNotExist\",\n" +
                              "    \"message\": \"The specified queue does not exist.\"\n" +
                              "}";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(400).withBody(responseBody)));
        List<CompletableFuture<SendMessageResponse>> responses = createAndSendSendMessageRequests(id1, id2);

        CompletableFuture<SendMessageResponse> response1 = responses.get(0);
        CompletableFuture<SendMessageResponse> response2 = responses.get(1);
        assertThatThrownBy(() -> response1.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(SqsException.class).hasMessageContaining("Status Code: 400");
        assertThatThrownBy(() -> response2.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(SqsException.class).hasMessageContaining("Status Code: 400");
    }

    @Test
    public void sendMessageBatchNetworkError_causesConnectionResetException() {
        String id1 = "0";
        String id2 = "1";
        String errorMessage = "Unable to execute HTTP request";
        stubFor(any(anyUrl()).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        List<CompletableFuture<SendMessageResponse>> responses = createAndSendSendMessageRequests(id1, id2);

        CompletableFuture<SendMessageResponse> response1 = responses.get(0);
        CompletableFuture<SendMessageResponse> response2 = responses.get(1);
        assertThatThrownBy(() -> response1.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(SdkClientException.class).hasMessageContaining(errorMessage);
        assertThatThrownBy(() -> response2.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(SdkClientException.class).hasMessageContaining(errorMessage);
    }

    @Test
    public void deleteMessageBatchFunction_batchMessageCorrectly() throws Exception {
        String id1 = "0";
        String id2 = "1";
        String responseBody = String.format(
            "{\"Successful\":[{\"Id\":\"%s\"},{\"Id\":\"%s\"}]}", id1, id2);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(responseBody)));

        long startTime = System.nanoTime();
        List<CompletableFuture<DeleteMessageResponse>> responses = createAndSendDeleteMessageRequests();
        long endTime = System.nanoTime();
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).get(3, TimeUnit.SECONDS);

        assertThat(Duration.ofNanos(endTime - startTime).toMillis()).isLessThan(DEFAULT_MAX_BATCH_OPEN + 100);

        verify(anyRequestedFor(anyUrl())
                   .withHeader("User-Agent", containing("hll/abm")));
    }

    @Test
    public void deleteMessageBatchFunctionWithBatchEntryFailures_wrapFailureMessageInBatchEntry() {
        String id1 = "0";
        String id2 = "1";
        String errorCode = "400";
        String errorMessage = "Some error";
        String responseBody = String.format(
            "{\n" +
            "    \"Failed\": [\n" +
            "        {\n" +
            "            \"Id\": \"%s\",\n" +
            "            \"Code\": \"%s\",\n" +
            "            \"Message\": \"%s\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"Id\": \"%s\",\n" +
            "            \"Code\": \"%s\",\n" +
            "            \"Message\": \"%s\"\n" +
            "        }\n" +
            "    ]\n" +
            "}",
            id1, errorCode, errorMessage, id2, errorCode, errorMessage
        );

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(responseBody)));
        List<CompletableFuture<DeleteMessageResponse>> responses = createAndSendDeleteMessageRequests();

        CompletableFuture<DeleteMessageResponse> response1 = responses.get(0);
        CompletableFuture<DeleteMessageResponse> response2 = responses.get(1);
        assertThatThrownBy(() -> response1.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(SqsException.class).hasMessageContaining(errorMessage);
        assertThatThrownBy(() -> response2.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(SqsException.class).hasMessageContaining(errorMessage);
    }

    @Test
    public void deleteMessageBatchFunctionReturnsWithError_completeMessagesExceptionally() {
        String responseBody = "{\n" +
                              "    \"__type\": \"com.amazonaws.sqs#QueueDoesNotExist\",\n" +
                              "    \"message\": \"The specified queue does not exist.\"\n" +
                              "}";
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(400).withBody(responseBody)));
        List<CompletableFuture<DeleteMessageResponse>> responses = createAndSendDeleteMessageRequests();

        CompletableFuture<DeleteMessageResponse> response1 = responses.get(0);
        CompletableFuture<DeleteMessageResponse> response2 = responses.get(1);
        assertThatThrownBy(() -> response1.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(SqsException.class).hasMessageContaining("Status Code: 400");
        assertThatThrownBy(() -> response2.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(SqsException.class).hasMessageContaining("Status Code: 400");
    }

    @Test
    public void changeVisibilityBatchFunction_batchMessageCorrectly() throws Exception {
        String id1 = "0";
        String id2 = "1";
        String responseBody = String.format(
            "{\"Successful\":[{\"Id\":\"%s\"},{\"Id\":\"%s\"}]}", id1, id2);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(responseBody)));
        long startTime = System.nanoTime();
        List<CompletableFuture<ChangeMessageVisibilityResponse>> responses = createAndSendChangeVisibilityRequests();
        long endTime = System.nanoTime();
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);

        assertThat(Duration.ofNanos(endTime - startTime).toMillis()).isLessThan(DEFAULT_MAX_BATCH_OPEN + 100);

        verify(anyRequestedFor(anyUrl())
                   .withHeader("User-Agent", containing("hll/abm")));

    }

    @Test
    public void changeVisibilityBatchFunctionWithBatchEntryFailures_wrapFailureMessageInBatchEntry() throws ExecutionException, InterruptedException, TimeoutException {
        String id1 = "0";
        String id2 = "1";
        String errorCode = "400";
        String errorMessage = "Some error";
        String responseBody = String.format(
            "{\n" +
            "    \"Failed\": [\n" +
            "        {\n" +
            "            \"Id\": \"%s\",\n" +
            "            \"Code\": \"%s\",\n" +
            "            \"Message\": \"%s\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"Id\": \"%s\",\n" +
            "            \"Code\": \"%s\",\n" +
            "            \"Message\": \"%s\"\n" +
            "        }\n" +
            "    ]\n" +
            "}",
            id1, errorCode, errorMessage, id2, errorCode, errorMessage
        );

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(responseBody)));
        List<CompletableFuture<ChangeMessageVisibilityResponse>> responses = createAndSendChangeVisibilityRequests();

        CompletableFuture<ChangeMessageVisibilityResponse> response1 = responses.get(0);
        CompletableFuture<ChangeMessageVisibilityResponse> response2 = responses.get(1);

        assertThatThrownBy(() -> response1.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(SqsException.class).hasMessageContaining(errorMessage);
        assertThatThrownBy(() -> response2.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(SqsException.class).hasMessageContaining(errorMessage);

    }


    @Test
    public void receieveBatchFunction_batchMessageCorrectly() throws Exception{
        String queueAttributeResponse = String.format(
            "{\n" +
            "    \"Attributes\": {\n" +
            "        \"ReceiveMessageWaitTimeSeconds\": \"%s\",\n" +
            "        \"VisibilityTimeout\": \"%s\"\n" +
            "    }\n" +
            "}",
            "0",
            "30"
        );


    String receiveBody = "{\n"
                         + "  \"Messages\": [\n"
                         + "    {\n"
                         + "      \"Body\": \"Message 5\",\n"
                         + "      \"MD5OfBody\": \"a7f5bea7c5781b5ccaf7585aa766aa4b\",\n"
                         + "      \"MessageId\": \"6fb1\",\n"
                         + "      \"ReceiptHandle\": \"AQEB\"\n"
                         + "    },\n"
                         + "    {\n"
                         + "      \"Body\": \"Message 6\",\n"
                         + "      \"MD5OfBody\": \"05d2a129ebdb00cfa6e92aaf9f090547\",\n"
                         + "      \"MessageId\": \"57d2\",\n"
                         + "      \"ReceiptHandle\": \"AQEB\"\n"
                         + "    }\n"
                         + "  ]\n"
                         + "}";


        stubFor(post(urlEqualTo("/"))
                    .withHeader("x-amz-target", equalTo("AmazonSQS.GetQueueAttributes"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody(queueAttributeResponse)));
        stubFor(post(urlEqualTo("/"))
                    .withHeader("x-amz-target", equalTo("AmazonSQS.ReceiveMessage"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody(receiveBody)));


        CompletableFuture<ReceiveMessageResponse> receiveMessage =
            createAndReceiveMessage(ReceiveMessageRequest.builder().queueUrl("queurl").build());

        ReceiveMessageResponse receiveMessageResponse = receiveMessage.get(1, TimeUnit.SECONDS);


        assertThat(receiveMessageResponse.messages()).hasSize(2);

        verify(anyRequestedFor(anyUrl())
                   .withHeader("User-Agent", containing("hll/abm")));
    }

    @Test
    public void changeVisibilityBatchFunctionReturnsWithError_completeMessagesExceptionally() {
        String responseBody = "{\n" +
                              "    \"__type\": \"com.amazonaws.sqs#QueueDoesNotExist\",\n" +
                              "    \"message\": \"The specified queue does not exist.\"\n" +
                              "}";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(400).withBody(responseBody)));
        List<CompletableFuture<ChangeMessageVisibilityResponse>> responses = createAndSendChangeVisibilityRequests();

        CompletableFuture<ChangeMessageVisibilityResponse> response1 = responses.get(0);
        CompletableFuture<ChangeMessageVisibilityResponse> response2 = responses.get(1);
        assertThatThrownBy(() -> response1.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(SqsException.class).hasMessageContaining("Status Code: 400");
        assertThatThrownBy(() -> response2.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(SqsException.class).hasMessageContaining("Status Code: 400");
    }

    public abstract List<CompletableFuture<SendMessageResponse>> createAndSendSendMessageRequests(String message1,
                                                                                                 String message2);

    public abstract CompletableFuture<ReceiveMessageResponse> createAndReceiveMessage(ReceiveMessageRequest request);

    public abstract List<CompletableFuture<DeleteMessageResponse>> createAndSendDeleteMessageRequests();

    public abstract List<CompletableFuture<ChangeMessageVisibilityResponse>> createAndSendChangeVisibilityRequests();

    private String getMd5Hash(String message) {
        byte[] expectedMd5;
        expectedMd5 = Md5Utils.computeMD5Hash(message.getBytes(StandardCharsets.UTF_8));
        return BinaryUtils.toHex(expectedMd5);
    }
}
