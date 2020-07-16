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
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.stability.tests.utils.StabilityTestRunner;
import software.amazon.awssdk.testutils.service.AwsTestBase;
import software.amazon.awssdk.utils.Logger;


public abstract class SqsBaseStabilityTest extends AwsTestBase {
    private static final Logger log = Logger.loggerFor(SqsNettyAsyncStabilityTest.class);
    protected static final int CONCURRENCY = 100;
    protected static final int TOTAL_RUNS = 50;


    protected abstract SqsAsyncClient getTestClient();
    protected abstract String getQueueUrl();
    protected abstract String getQueueName();

    protected static String setup(SqsAsyncClient client, String queueName) {
        CreateQueueResponse createQueueResponse = client.createQueue(b -> b.queueName(queueName)).join();
        return createQueueResponse.queueUrl();
    }

    protected static void tearDown(SqsAsyncClient client, String queueUrl) {
        if (queueUrl != null) {
            client.deleteQueue(b -> b.queueUrl(queueUrl));
        }
    }

    protected void sendMessage() {
        log.info(() -> String.format("Starting testing sending messages to queue %s with queueUrl %s", getQueueName(), getQueueUrl()));
        String messageBody = RandomStringUtils.randomAscii(1000);
        IntFunction<CompletableFuture<?>> futureIntFunction =
                i -> getTestClient().sendMessage(b -> b.queueUrl(getQueueUrl()).messageBody(messageBody));

        runSqsTests("sendMessage", futureIntFunction);
    }

    protected void receiveMessage() {
        log.info(() -> String.format("Starting testing receiving messages from queue %s with queueUrl %s", getQueueName(), getQueueUrl()));
        IntFunction<CompletableFuture<?>> futureIntFunction =
                i -> getTestClient().receiveMessage(b -> b.queueUrl(getQueueUrl()))
                        .thenApply(
                                r -> {
                                    List<DeleteMessageBatchRequestEntry> batchRequestEntries =
                                            r.messages().stream().map(m -> DeleteMessageBatchRequestEntry.builder().id(m.messageId()).receiptHandle(m.receiptHandle()).build())
                                                    .collect(Collectors.toList());
                                    return getTestClient().deleteMessageBatch(b -> b.queueUrl(getQueueUrl()).entries(batchRequestEntries));
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
