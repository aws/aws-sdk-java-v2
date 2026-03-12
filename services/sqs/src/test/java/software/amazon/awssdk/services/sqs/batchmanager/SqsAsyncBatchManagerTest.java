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


import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;


@ExtendWith(MockitoExtension.class)
public class SqsAsyncBatchManagerTest extends BaseSqsBatchManagerTest {

    private static SqsAsyncClient client;
    private SqsAsyncBatchManager batchManager;


    private static SqsAsyncClientBuilder getAsyncClientBuilder(URI httpLocalhostUri) {
        return SqsAsyncClient.builder()
                             .endpointOverride(httpLocalhostUri)
                             .credentialsProvider(
                                 StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")));
    }

    @BeforeAll
    public static void oneTimeSetUp() {
        URI http_localhost_uri = URI.create(String.format("http://localhost:%s/", wireMock.getPort()));
        client = getAsyncClientBuilder(http_localhost_uri).build();
    }

    @AfterAll
    public static void oneTimeTearDown() {
        client.close();
    }

    @BeforeEach
    public void setUp() {
        batchManager = client.batchManager();
    }

    @AfterEach
    public void tearDown() {
        batchManager.close();
    }


    @Override
    public List<CompletableFuture<SendMessageResponse>> createAndSendSendMessageRequests(String message1, String message2) {
        List<CompletableFuture<SendMessageResponse>> responses = new ArrayList<>();
        responses.add(batchManager.sendMessage(builder -> builder.queueUrl(DEFAULT_QUEUE_URL).messageBody(message1)));
        responses.add(batchManager.sendMessage(builder -> builder.queueUrl(DEFAULT_QUEUE_URL).messageBody(message2)));
        return responses;
    }

    @Override
    public CompletableFuture<ReceiveMessageResponse> createAndReceiveMessage(ReceiveMessageRequest request) {
        return batchManager.receiveMessage(request);
    }

    @Override
    public List<CompletableFuture<DeleteMessageResponse>> createAndSendDeleteMessageRequests() {
        List<DeleteMessageRequest> requests = new ArrayList<>();
        List<CompletableFuture<DeleteMessageResponse>> responses = new ArrayList<>();
        responses.add(batchManager.deleteMessage(builder -> builder.queueUrl(DEFAULT_QUEUE_URL)));
        responses.add(batchManager.deleteMessage(builder -> builder.queueUrl(DEFAULT_QUEUE_URL)));
        return responses;
    }

    @Override
    public List<CompletableFuture<ChangeMessageVisibilityResponse>> createAndSendChangeVisibilityRequests() {
        List<CompletableFuture<ChangeMessageVisibilityResponse>> responses = new ArrayList<>();
        responses.add(batchManager.changeMessageVisibility(builder -> builder.queueUrl(DEFAULT_QUEUE_URL)));
        responses.add(batchManager.changeMessageVisibility(builder -> builder.queueUrl(DEFAULT_QUEUE_URL)));
        return responses;
    }
}
