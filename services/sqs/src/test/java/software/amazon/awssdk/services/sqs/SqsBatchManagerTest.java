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

package software.amazon.awssdk.services.sqs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.batchmanager.BatchManager;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.batchmanager.SqsBatchManager;
import software.amazon.awssdk.services.sqs.internal.batchmanager.DefaultSqsBatchManager;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Md5Utils;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

@RunWith(MockitoJUnitRunner.class)
public class SqsBatchManagerTest {

    private static final int DEFAULT_MAX_BATCH_OPEN = 200;
    private static final String DEFAULT_QUEUE_URl = "SomeQueueUrl";
    private static final URI HTTP_LOCALHOST_URI = URI.create("http://localhost:8080/");
    private static ScheduledExecutorService scheduledExecutor;
    private static ExecutorService executor;
    private static SqsClient client;
    private SqsBatchManager batchManager;

    @Rule
    public WireMockRule wireMock = new WireMockRule();

    private static SqsClientBuilder getSyncClientBuilder() {
        return SqsClient.builder()
                        .region(Region.US_EAST_1)
                        .endpointOverride(HTTP_LOCALHOST_URI)
                        .credentialsProvider(
                            StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")));
    }

    @BeforeClass
    public static void oneTimeSetUp() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("SqsBatchManager").build();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        executor = Executors.newSingleThreadExecutor();
        client = getSyncClientBuilder().build();
    }

    @AfterClass
    public static void oneTimeTearDown() {
        client.close();
        scheduledExecutor.shutdownNow();
        executor.shutdownNow();
    }

    @Before
    public void setUp() {
        batchManager = SqsBatchManager.builder()
                                      .client(client)
                                      .scheduledExecutor(scheduledExecutor)
                                      .executor(executor)
                                      .build();
    }

    @After
    public void tearDown() {
        batchManager.close();
    }

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

        SendMessageResponse completedResponse1 = responses.get(0).join();
        SendMessageResponse completedResponse2 = responses.get(1).join();
        String expectedHash = getMd5Hash(String.format("%s: %s", errorCode, errorMessage));
        assertThat(completedResponse1.md5OfMessageBody()).isEqualTo(expectedHash);
        assertThat(completedResponse2.md5OfMessageBody()).isEqualTo(expectedHash);
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
        assertThatThrownBy(response1::join).isInstanceOf(CompletionException.class).hasMessageContaining("400");
        assertThatThrownBy(response2::join).isInstanceOf(CompletionException.class).hasMessageContaining("400");
    }

    @Test
    public void sendMessageBatchNetworkError_causesConnectionResetException() {
        String id1 = "0";
        String id2 = "1";
        stubFor(any(anyUrl()).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        List<CompletableFuture<SendMessageResponse>> responses = createAndSendSendMessageRequests(id1, id2);

        CompletableFuture<SendMessageResponse> response1 = responses.get(0);
        CompletableFuture<SendMessageResponse> response2 = responses.get(1);
        assertThatThrownBy(response1::join).isInstanceOf(CompletionException.class).hasMessageContaining("Connection reset");
        assertThatThrownBy(response2::join).isInstanceOf(CompletionException.class).hasMessageContaining("Connection reset");
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

        List<DeleteMessageRequest> requests = new ArrayList<>();
        requests.add(createDeleteMessageRequest());
        requests.add(createDeleteMessageRequest());

        List<CompletableFuture<DeleteMessageResponse>> responses = new ArrayList<>();
        long startTime = System.nanoTime();
        for (DeleteMessageRequest request : requests) {
            responses.add(batchManager.deleteMessage(request));
        }
        long endTime = System.nanoTime();
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join();

        assertThat(Duration.ofNanos(endTime - startTime).toMillis()).isLessThan(DEFAULT_MAX_BATCH_OPEN + 100);
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

        List<DeleteMessageRequest> requests = new ArrayList<>();
        requests.add(createDeleteMessageRequest());
        requests.add(createDeleteMessageRequest());

        List<CompletableFuture<DeleteMessageResponse>> responses = new ArrayList<>();
        for (DeleteMessageRequest request : requests) {
            responses.add(batchManager.deleteMessage(request));
        }

        CompletableFuture<DeleteMessageResponse> response1 = responses.get(0);
        CompletableFuture<DeleteMessageResponse> response2 = responses.get(1);
        assertThatThrownBy(response1::join).isInstanceOf(CompletionException.class).hasMessageContaining("400");
        assertThatThrownBy(response2::join).isInstanceOf(CompletionException.class).hasMessageContaining("400");
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

        List<ChangeMessageVisibilityRequest> requests = new ArrayList<>();
        requests.add(createChangeVisibilityRequest());
        requests.add(createChangeVisibilityRequest());

        List<CompletableFuture<ChangeMessageVisibilityResponse>> responses = new ArrayList<>();
        long startTime = System.nanoTime();
        for (ChangeMessageVisibilityRequest request : requests) {
            responses.add(batchManager.changeMessageVisibility(request));
        }
        long endTime = System.nanoTime();
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join();

        assertThat(Duration.ofNanos(endTime - startTime).toMillis()).isLessThan(DEFAULT_MAX_BATCH_OPEN + 100);
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

        List<ChangeMessageVisibilityRequest> requests = new ArrayList<>();
        requests.add(createChangeVisibilityRequest());
        requests.add(createChangeVisibilityRequest());

        List<CompletableFuture<ChangeMessageVisibilityResponse>> responses = new ArrayList<>();
        for (ChangeMessageVisibilityRequest request : requests) {
            responses.add(batchManager.changeMessageVisibility(request));
        }

        CompletableFuture<ChangeMessageVisibilityResponse> response1 = responses.get(0);
        CompletableFuture<ChangeMessageVisibilityResponse> response2 = responses.get(1);
        assertThatThrownBy(response1::join).isInstanceOf(CompletionException.class).hasMessageContaining("400");
        assertThatThrownBy(response2::join).isInstanceOf(CompletionException.class).hasMessageContaining("400");
    }

    @Mock
    private BatchManager<SendMessageRequest, SendMessageResponse, SendMessageBatchResponse> mockSendMessageBatchManager;

    @Mock
    private BatchManager<DeleteMessageRequest, DeleteMessageResponse, DeleteMessageBatchResponse> mockDeleteMessageBatchManager;

    @Mock
    private BatchManager<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResponse,
        ChangeMessageVisibilityBatchResponse> mockChangeVisibilityBatchManager;

    @Test
    public void closeBatchManager_shouldNotCloseExecutorsOrClient() {
        SqsBatchManager batchManager = new DefaultSqsBatchManager(client, executor, mockSendMessageBatchManager,
                                                                  mockDeleteMessageBatchManager,
                                                                  mockChangeVisibilityBatchManager);
        batchManager.close();
        verify(mockSendMessageBatchManager).close();
        verify(mockDeleteMessageBatchManager).close();
        verify(mockChangeVisibilityBatchManager).close();
        assertThat(executor.isShutdown()).isFalse();
    }

    private List<CompletableFuture<SendMessageResponse>> createAndSendSendMessageRequests(String message1, String message2) {
        List<SendMessageRequest> requests = new ArrayList<>();
        requests.add(createSendMessageRequest(message1));
        requests.add(createSendMessageRequest(message2));

        List<CompletableFuture<SendMessageResponse>> responses = new ArrayList<>();
        for (SendMessageRequest request : requests) {
            responses.add(batchManager.sendMessage(request));
        }
        return responses;
    }

    private SendMessageRequest createSendMessageRequest(String messageBody) {
        return SendMessageRequest.builder()
                                 .messageBody(messageBody)
                                 .queueUrl(DEFAULT_QUEUE_URl)
                                 .build();
    }

    private DeleteMessageRequest createDeleteMessageRequest() {
        return DeleteMessageRequest.builder()
                                   .queueUrl(DEFAULT_QUEUE_URl)
                                   .build();
    }

    private ChangeMessageVisibilityRequest createChangeVisibilityRequest() {
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
