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

package software.amazon.awssdk.services.sqs.batchmanager.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.ClassRule;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Md5Utils;

public abstract class BaseSqsBatchManagerTest {

    private static final String DEFAULT_QUEUE_URl = "SomeQueueUrl";
    private static final int DEFAULT_MAX_BATCH_OPEN = 200;

    @ClassRule
    public static WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    @Test
    public void sendMessageBatchFunction_batchMessageCorrectly() {
        String id1 = "0";
        String id2 = "1";
        String messageBody1 = getMd5Hash(id1);
        String messageBody2 = getMd5Hash(id2);
        String responseBody = String.format(
            "<SendMessageBatchResponse>\n"
            + "<SendMessageBatchResult>\n"
            + "    <SendMessageBatchResultEntry>\n"
            + "        <Id>%s</Id>\n"
            + "        <MD5OfMessageBody>%s</MD5OfMessageBody>\n"
            + "    </SendMessageBatchResultEntry>\n"
            + "    <SendMessageBatchResultEntry>\n"
            + "        <Id>%s</Id>\n"
            + "        <MD5OfMessageBody>%s</MD5OfMessageBody>\n"
            + "    </SendMessageBatchResultEntry>\n"
            + "</SendMessageBatchResult>\n"
            + "</SendMessageBatchResponse>", id1, messageBody1, id2, messageBody2);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(responseBody)));
        List<CompletableFuture<SendMessageResponse>> responses = createAndSendSendMessageRequests(id1, id2);

        SendMessageResponse completedResponse1 = responses.get(0).join();
        SendMessageResponse completedResponse2 = responses.get(1).join();
        assertThat(completedResponse1.md5OfMessageBody()).isEqualTo(messageBody1);
        assertThat(completedResponse2.md5OfMessageBody()).isEqualTo(messageBody2);
    }

    @Test
    public void sendMessageBatchFunctionWithBatchEntryFailures_wrapFailureMessageInBatchEntry() {
        String id1 = "0";
        String id2 = "1";
        String errorCode = "400";
        String errorMessage = "Some error";
        String responseBody = String.format(
            "<SendMessageBatchResponse>\n"
            + "<SendMessageBatchResult>\n"
            + "    <BatchResultErrorEntry>\n"
            + "        <Id>%s</Id>\n"
            + "        <Code>%s</Code>\n"
            + "        <Message>%s</Message>\n"
            + "    </BatchResultErrorEntry>\n"
            + "    <BatchResultErrorEntry>\n"
            + "        <Id>%s</Id>\n"
            + "        <Code>%s</Code>\n"
            + "        <Message>%s</Message>\n"
            + "    </BatchResultErrorEntry>\n"
            + "</SendMessageBatchResult>\n"
            + "</SendMessageBatchResponse>", id1, errorCode, errorMessage, id2, errorCode, errorMessage);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(responseBody)));
        List<CompletableFuture<SendMessageResponse>> responses = createAndSendSendMessageRequests(id1, id2);

        CompletableFuture<SendMessageResponse> response1 = responses.get(0);
        CompletableFuture<SendMessageResponse> response2 = responses.get(1);
        assertThatThrownBy(response1::join).hasCauseInstanceOf(SqsException.class).hasMessageContaining(errorMessage);
        assertThatThrownBy(response2::join).hasCauseInstanceOf(SqsException.class).hasMessageContaining(errorMessage);
    }

    @Test
    public void sendMessageBatchFunctionReturnsWithError_completeMessagesExceptionally() {
        String id1 = "0";
        String id2 = "1";
        String responseBody = "<Error>\n"
                              + "<Code>CustomError</Code>\n"
                              + "<Message>Foo bar</Message>\n"
                              + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                              + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                              + "</Error>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(400).withBody(responseBody)));
        List<CompletableFuture<SendMessageResponse>> responses = createAndSendSendMessageRequests(id1, id2);

        CompletableFuture<SendMessageResponse> response1 = responses.get(0);
        CompletableFuture<SendMessageResponse> response2 = responses.get(1);
        assertThatThrownBy(response1::join).hasCauseInstanceOf(SqsException.class).hasMessageContaining("Status Code: 400");
        assertThatThrownBy(response2::join).hasCauseInstanceOf(SqsException.class).hasMessageContaining("Status Code: 400");
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
        assertThatThrownBy(response1::join).hasCauseInstanceOf(SdkClientException.class).hasMessageContaining(errorMessage);
        assertThatThrownBy(response2::join).hasCauseInstanceOf(SdkClientException.class).hasMessageContaining(errorMessage);
    }

    @Test
    public void deleteMessageBatchFunction_batchMessageCorrectly() {
        String id1 = "0";
        String id2 = "1";
        String responseBody = String.format(
            "<DeleteMessageBatchResponse>\n"
            + "    <DeleteMessageBatchResult>\n"
            + "        <DeleteMessageBatchResultEntry>\n"
            + "            <Id>%s</Id>\n"
            + "        </DeleteMessageBatchResultEntry>\n"
            + "        <DeleteMessageBatchResultEntry>\n"
            + "            <Id>%s</Id>\n"
            + "        </DeleteMessageBatchResultEntry>\n"
            + "    </DeleteMessageBatchResult>\n"
            + "</DeleteMessageBatchResponse>\n", id1, id2);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(responseBody)));

        long startTime = System.nanoTime();
        List<CompletableFuture<DeleteMessageResponse>> responses = createAndSendDeleteMessageRequests();
        long endTime = System.nanoTime();
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join();

        assertThat(Duration.ofNanos(endTime - startTime).toMillis()).isLessThan(DEFAULT_MAX_BATCH_OPEN + 100);
    }

    @Test
    public void deleteMessageBatchFunctionWithBatchEntryFailures_wrapFailureMessageInBatchEntry() {
        String id1 = "0";
        String id2 = "1";
        String errorCode = "400";
        String errorMessage = "Some error";
        String responseBody = String.format(
            "<DeleteMessageBatchResponse>\n"
            + "<DeleteMessageBatchResult>\n"
            + "    <BatchResultErrorEntry>\n"
            + "        <Id>%s</Id>\n"
            + "        <Code>%s</Code>\n"
            + "        <Message>%s</Message>\n"
            + "    </BatchResultErrorEntry>\n"
            + "    <BatchResultErrorEntry>\n"
            + "        <Id>%s</Id>\n"
            + "        <Code>%s</Code>\n"
            + "        <Message>%s</Message>\n"
            + "    </BatchResultErrorEntry>\n"
            + "</DeleteMessageBatchResult>\n"
            + "</DeleteMessageBatchResponse>", id1, errorCode, errorMessage, id2, errorCode, errorMessage);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(responseBody)));
        List<CompletableFuture<DeleteMessageResponse>> responses = createAndSendDeleteMessageRequests();

        CompletableFuture<DeleteMessageResponse> response1 = responses.get(0);
        CompletableFuture<DeleteMessageResponse> response2 = responses.get(1);
        assertThatThrownBy(response1::join).hasCauseInstanceOf(SqsException.class).hasMessageContaining(errorMessage);
        assertThatThrownBy(response2::join).hasCauseInstanceOf(SqsException.class).hasMessageContaining(errorMessage);
    }

    @Test
    public void deleteMessageBatchFunctionReturnsWithError_completeMessagesExceptionally() {
        String responseBody = "<Error>\n"
                              + "<Code>CustomError</Code>\n"
                              + "<Message>Foo bar</Message>\n"
                              + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                              + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                              + "</Error>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(400).withBody(responseBody)));
        List<CompletableFuture<DeleteMessageResponse>> responses = createAndSendDeleteMessageRequests();

        CompletableFuture<DeleteMessageResponse> response1 = responses.get(0);
        CompletableFuture<DeleteMessageResponse> response2 = responses.get(1);
        assertThatThrownBy(response1::join).hasCauseInstanceOf(SqsException.class).hasMessageContaining("Status Code: 400");
        assertThatThrownBy(response2::join).hasCauseInstanceOf(SqsException.class).hasMessageContaining("Status Code: 400");
    }

    @Test
    public void changeVisibilityBatchFunction_batchMessageCorrectly() {
        String id1 = "0";
        String id2 = "1";
        String responseBody = String.format(
            "<ChangeMessageVisibilityBatchResponse>\n"
            + "    <ChangeMessageVisibilityBatchResult>\n"
            + "        <ChangeMessageVisibilityBatchResultEntry>\n"
            + "            <Id>%s</Id>\n"
            + "        </ChangeMessageVisibilityBatchResultEntry>\n"
            + "        <ChangeMessageVisibilityBatchResultEntry>\n"
            + "            <Id>%s</Id>\n"
            + "        </ChangeMessageVisibilityBatchResultEntry>\n"
            + "    </ChangeMessageVisibilityBatchResult>\n"
            + "</ChangeMessageVisibilityBatchResponse>", id1, id2);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(responseBody)));
        long startTime = System.nanoTime();
        List<CompletableFuture<ChangeMessageVisibilityResponse>> responses = createAndSendChangeVisibilityRequests();
        long endTime = System.nanoTime();
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join();

        assertThat(Duration.ofNanos(endTime - startTime).toMillis()).isLessThan(DEFAULT_MAX_BATCH_OPEN + 100);
    }

    @Test
    public void changeVisibilityBatchFunctionWithBatchEntryFailures_wrapFailureMessageInBatchEntry() {
        String id1 = "0";
        String id2 = "1";
        String errorCode = "400";
        String errorMessage = "Some error";
        String responseBody = String.format(
            "<ChangeMessageVisibilityBatchResponse>\n"
            + "<ChangeMessageVisibilityBatchResult>\n"
            + "    <BatchResultErrorEntry>\n"
            + "        <Id>%s</Id>\n"
            + "        <Code>%s</Code>\n"
            + "        <Message>%s</Message>\n"
            + "    </BatchResultErrorEntry>\n"
            + "    <BatchResultErrorEntry>\n"
            + "        <Id>%s</Id>\n"
            + "        <Code>%s</Code>\n"
            + "        <Message>%s</Message>\n"
            + "    </BatchResultErrorEntry>\n"
            + "</ChangeMessageVisibilityBatchResult>\n"
            + "</ChangeMessageVisibilityBatchResponse>", id1, errorCode, errorMessage, id2, errorCode, errorMessage);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(responseBody)));
        List<CompletableFuture<ChangeMessageVisibilityResponse>> responses = createAndSendChangeVisibilityRequests();

        CompletableFuture<ChangeMessageVisibilityResponse> response1 = responses.get(0);
        CompletableFuture<ChangeMessageVisibilityResponse> response2 = responses.get(1);
        assertThatThrownBy(response1::join).hasCauseInstanceOf(SqsException.class).hasMessageContaining(errorMessage);
        assertThatThrownBy(response2::join).hasCauseInstanceOf(SqsException.class).hasMessageContaining(errorMessage);
    }

    @Test
    public void changeVisibilityBatchFunctionReturnsWithError_completeMessagesExceptionally() {
        String responseBody = "<Error>\n"
                              + "<Code>CustomError</Code>\n"
                              + "<Message>Foo bar</Message>\n"
                              + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                              + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                              + "</Error>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(400).withBody(responseBody)));
        List<CompletableFuture<ChangeMessageVisibilityResponse>> responses = createAndSendChangeVisibilityRequests();

        CompletableFuture<ChangeMessageVisibilityResponse> response1 = responses.get(0);
        CompletableFuture<ChangeMessageVisibilityResponse> response2 = responses.get(1);
        assertThatThrownBy(response1::join).hasCauseInstanceOf(SqsException.class).hasMessageContaining("Status Code: 400");
        assertThatThrownBy(response2::join).hasCauseInstanceOf(SqsException.class).hasMessageContaining("Status Code: 400");
    }

    public abstract List<CompletableFuture<SendMessageResponse>> createAndSendSendMessageRequests(String message1,
                                                                                                 String message2);

    public abstract List<CompletableFuture<DeleteMessageResponse>> createAndSendDeleteMessageRequests();

    public abstract List<CompletableFuture<ChangeMessageVisibilityResponse>> createAndSendChangeVisibilityRequests();

    SendMessageRequest createSendMessageRequest(String messageBody) {
        return SendMessageRequest.builder()
                                 .messageBody(messageBody)
                                 .queueUrl(DEFAULT_QUEUE_URl)
                                 .build();
    }

    DeleteMessageRequest createDeleteMessageRequest() {
        return DeleteMessageRequest.builder()
                                   .queueUrl(DEFAULT_QUEUE_URl)
                                   .build();
    }

    ChangeMessageVisibilityRequest createChangeVisibilityRequest() {
        return ChangeMessageVisibilityRequest.builder()
                                             .queueUrl(DEFAULT_QUEUE_URl)
                                             .build();
    }

    private String getMd5Hash(String message) {
        byte[] expectedMd5;
        expectedMd5 = Md5Utils.computeMD5Hash(message.getBytes(StandardCharsets.UTF_8));
        return BinaryUtils.toHex(expectedMd5);
    }
}
