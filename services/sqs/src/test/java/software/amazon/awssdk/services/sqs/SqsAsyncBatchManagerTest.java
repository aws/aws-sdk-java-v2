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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.batchmanager.BatchManager;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.internal.batchmanager.DefaultSqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

@RunWith(MockitoJUnitRunner.class)
public class SqsAsyncBatchManagerTest extends BaseSqsBatchManagerTest {

    private static SqsAsyncClient client;
    private SqsAsyncBatchManager batchManager;

    @Mock
    private BatchManager<SendMessageRequest, SendMessageResponse, SendMessageBatchResponse> mockSendMessageBatchManager;

    @Mock
    private BatchManager<DeleteMessageRequest, DeleteMessageResponse, DeleteMessageBatchResponse> mockDeleteMessageBatchManager;

    @Mock
    private BatchManager<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResponse,
        ChangeMessageVisibilityBatchResponse> mockChangeVisibilityBatchManager;

    private static SqsAsyncClientBuilder getAsyncClientBuilder(URI http_localhost_uri) {
        return SqsAsyncClient.builder()
                             .region(Region.US_EAST_1)
                             .endpointOverride(http_localhost_uri)
                             .credentialsProvider(
                                 StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")));
    }

    @BeforeClass
    public static void oneTimeSetUp() {
        URI http_localhost_uri = URI.create(String.format("http://localhost:%s/", wireMock.port()));
        client = getAsyncClientBuilder(http_localhost_uri).build();
    }

    @AfterClass
    public static void oneTimeTearDown() {
        client.close();
    }

    @Before
    public void setUp() {
        batchManager = client.batchManager();
    }

    @After
    public void tearDown() {
        batchManager.close();
    }

    @Test
    public void closeBatchManager_shouldNotCloseExecutorsOrClient() {
        SqsAsyncBatchManager batchManager = new DefaultSqsAsyncBatchManager(client,
                                                                            mockSendMessageBatchManager,
                                                                            mockDeleteMessageBatchManager,
                                                                            mockChangeVisibilityBatchManager);
        batchManager.close();
        verify(mockSendMessageBatchManager).close();
        verify(mockDeleteMessageBatchManager).close();
        verify(mockChangeVisibilityBatchManager).close();
        assertThatCode(() -> client.serviceName()).doesNotThrowAnyException(); // To make sure client is not closed
    }

    @Override
    public List<CompletableFuture<SendMessageResponse>> createAndSendSendMessageRequests(String message1, String message2) {
        List<SendMessageRequest> requests = new ArrayList<>();
        requests.add(createSendMessageRequest(message1));
        requests.add(createSendMessageRequest(message2));

        List<CompletableFuture<SendMessageResponse>> responses = new ArrayList<>();
        for (SendMessageRequest request : requests) {
            responses.add(batchManager.sendMessage(request));
        }
        return responses;
    }

    @Override
    public List<CompletableFuture<DeleteMessageResponse>> createAndSendDeleteMessageRequests() {
        List<DeleteMessageRequest> requests = new ArrayList<>();
        requests.add(createDeleteMessageRequest());
        requests.add(createDeleteMessageRequest());
        List<CompletableFuture<DeleteMessageResponse>> responses = new ArrayList<>();

        for (DeleteMessageRequest request : requests) {
            responses.add(batchManager.deleteMessage(request));
        }
        return responses;
    }

    @Override
    public List<CompletableFuture<ChangeMessageVisibilityResponse>> createAndSendChangeVisibilityRequests() {
        List<ChangeMessageVisibilityRequest> requests = new ArrayList<>();
        requests.add(createChangeVisibilityRequest());
        requests.add(createChangeVisibilityRequest());

        List<CompletableFuture<ChangeMessageVisibilityResponse>> responses = new ArrayList<>();
        for (ChangeMessageVisibilityRequest request : requests) {
            responses.add(batchManager.changeMessageVisibility(request));
        }

        return responses;
    }
}
