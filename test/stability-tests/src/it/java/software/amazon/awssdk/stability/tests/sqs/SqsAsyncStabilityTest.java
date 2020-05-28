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

package software.amazon.awssdk.stability.tests.sqs;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.stability.tests.exceptions.StabilityTestsRetryableException;
import software.amazon.awssdk.stability.tests.utils.RetryableTest;
import software.amazon.awssdk.stability.tests.utils.StabilityTestRunner;
import software.amazon.awssdk.utils.Logger;

public class SqsAsyncStabilityTest extends SqsBaseStabilityTest {
    private static final Logger log = Logger.loggerFor(SqsAsyncStabilityTest.class);
    private static String queueName;
    private static String queueUrl;

    @BeforeAll
    public static void setup() {
        queueName = "sqsasyncstabilitytests" + System.currentTimeMillis();
        CreateQueueResponse createQueueResponse = sqsAsyncClient.createQueue(b -> b.queueName(queueName)).join();
        queueUrl = createQueueResponse.queueUrl();
    }

    @AfterAll
    public static void tearDown() {
        if (queueUrl != null) {
            sqsAsyncClient.deleteQueue(b -> b.queueUrl(queueUrl));
        }
        sqsAsyncClient.close();
    }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void sendMessage_receiveMessage() {
        sendMessage();
        receiveMessage();
    }

    private void sendMessage() {
        log.info(() -> String.format("Starting testing sending messages to queue %s with queueUrl %s", queueName, queueUrl));
        String messageBody = RandomStringUtils.randomAscii(1000);
        IntFunction<CompletableFuture<?>> futureIntFunction =
            i -> sqsAsyncClient.sendMessage(b -> b.queueUrl(queueUrl).messageBody(messageBody));

        runSqsTests("sendMessage", futureIntFunction);
    }

    private void receiveMessage() {
        log.info(() -> String.format("Starting testing receiving messages from queue %s with queueUrl %s", queueName, queueUrl));
        IntFunction<CompletableFuture<?>> futureIntFunction =
            i -> sqsAsyncClient.receiveMessage(b -> b.queueUrl(queueUrl))
                               .thenApply(
                                   r -> {
                                       List<DeleteMessageBatchRequestEntry> batchRequestEntries =
                                           r.messages().stream().map(m -> DeleteMessageBatchRequestEntry.builder().id(m.messageId()).receiptHandle(m.receiptHandle()).build())
                                            .collect(Collectors.toList());
                                       return sqsAsyncClient.deleteMessageBatch(b -> b.queueUrl(queueUrl).entries(batchRequestEntries));
                                   });
        runSqsTests("receiveMessage", futureIntFunction);
    }

    private void runSqsTests(String testName, IntFunction<CompletableFuture<?>> futureIntFunction) {
        StabilityTestRunner.newRunner()
                           .testName("SqsAsyncStabilityTest." + testName)
                           .futureFactory(futureIntFunction)
                           .totalRuns(TOTAL_RUNS)
                           .requestCountPerRun(CONCURRENCY)
                           .delaysBetweenEachRun(Duration.ofMillis(100))
                           .run();
    }
}
