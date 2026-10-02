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

package software.amazon.awssdk.stability.tests.kinesis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.ConsumerStatus;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.ResourceInUseException;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEvent;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEventStream;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponse;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponseHandler;
import software.amazon.awssdk.stability.tests.utils.StabilityTestRunner;
import software.amazon.awssdk.stability.tests.utils.TestEventStreamingResponseHandler;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.testutils.service.AwsTestBase;
import software.amazon.awssdk.utils.Logger;

/**
 * Base class for Kinesis stability tests.
 */
public abstract class KinesisBaseStabilityTest extends AwsTestBase {
    private static final Logger log = Logger.loggerFor(KinesisBaseStabilityTest.class);
    protected static final int CONSUMER_COUNT = 4;
    protected static final int SHARD_COUNT = 9;
    protected static final int CONCURRENCY = CONSUMER_COUNT * SHARD_COUNT;
    protected static final int MAX_CONCURRENCY = CONCURRENCY + 10;

    protected List<String> consumerArns;
    protected List<String> shardIds;
    protected List<SdkBytes> producedData;
    protected KinesisAsyncClient asyncClient;
    protected String streamName;
    protected String streamARN;
    protected ExecutorService waiterExecutorService;
    protected ScheduledExecutorService producer;

    protected abstract KinesisAsyncClient createClient();
    protected abstract String getTestNamePrefix();
    protected abstract String getConsumerPrefix();

    @BeforeEach
    public void setup() {
        streamName = getTestNamePrefix().toLowerCase() + System.currentTimeMillis();
        consumerArns = new ArrayList<>(CONSUMER_COUNT);
        shardIds = new ArrayList<>(SHARD_COUNT);
        producedData = new ArrayList<>();

        asyncClient = createClient();

        asyncClient.createStream(r -> r.streamName(streamName).shardCount(SHARD_COUNT)).join();
        waitForStreamToBeActive();

        streamARN = asyncClient.describeStream(r -> r.streamName(streamName)).join()
                               .streamDescription()
                               .streamARN();

        shardIds = asyncClient.listShards(r -> r.streamName(streamName))
                              .join()
                              .shards().stream().map(Shard::shardId).collect(Collectors.toList());

        waiterExecutorService = Executors.newFixedThreadPool(CONSUMER_COUNT);
        producer = Executors.newScheduledThreadPool(1);
        registerStreamConsumers();
        waitForConsumersToBeActive();
    }

    @AfterEach
    public void tearDown() {
        asyncClient.deleteStream(b -> b.streamName(streamName).enforceConsumerDeletion(true)).join();
        waiterExecutorService.shutdown();
        producer.shutdown();
        asyncClient.close();
    }

    protected void runPutRecordsAndSubscribeToShard() throws InterruptedException {
        putRecords();
        subscribeToShard();
    }

    private void subscribeToShard() throws InterruptedException {
        log.info(() -> "Starting subscribeToShard to stream: " + streamName);
        List<CompletableFuture<?>> completableFutures = generateSubscribeToShardFutures();
        StabilityTestRunner.newRunner()
                           .testName(getTestNamePrefix() + ".subscribeToShard")
                           .futures(completableFutures)
                           .run();
    }

    private void registerStreamConsumers() {
        log.info(() -> "Registering stream consumers for " + streamARN);
        IntFunction<CompletableFuture<?>> futureFunction = i ->
            asyncClient.registerStreamConsumer(r -> r.streamARN(streamARN).consumerName(getConsumerPrefix() + i))
                       .thenApply(b -> consumerArns.add(b.consumer().consumerARN()));

        StabilityTestRunner.newRunner()
                           .requestCountPerRun(CONSUMER_COUNT)
                           .totalRuns(1)
                           .testName(getTestNamePrefix() + ".registerStreamConsumers")
                           .futureFactory(futureFunction)
                           .run();
    }

    private void putRecords() {
        log.info(() -> "Starting putRecord");
        producedData = new ArrayList<>();
        SdkBytes data = SdkBytes.fromByteArray(RandomUtils.nextBytes(20));
        IntFunction<CompletableFuture<?>> futureFunction = i ->
            asyncClient.putRecord(PutRecordRequest.builder()
                                                  .streamName(streamName)
                                                  .data(data)
                                                  .partitionKey(UUID.randomUUID().toString())
                                                  .build())
                       .thenApply(b -> producedData.add(data));

        StabilityTestRunner.newRunner()
                           .requestCountPerRun(CONCURRENCY)
                           .testName(getTestNamePrefix() + ".putRecords")
                           .futureFactory(futureFunction)
                           .run();
    }

    private List<CompletableFuture<?>> generateSubscribeToShardFutures() throws InterruptedException {
        List<CompletableFuture<?>> completableFutures = new ArrayList<>();
        int baseDelay = 150;
        int jitterRange = 150;
        for (int i = 0; i < CONSUMER_COUNT; i++) {
            final int consumerIndex = i;
            for (int j = 0; j < SHARD_COUNT; j++) {
                final int shardIndex = j;
                Thread.sleep(baseDelay + (int) (Math.random() * jitterRange));
                TestSubscribeToShardResponseHandler responseHandler =
                    new TestSubscribeToShardResponseHandler(consumerIndex, shardIndex);
                CompletableFuture<Void> completableFuture =
                    asyncClient.subscribeToShard(b -> b.shardId(shardIds.get(shardIndex))
                                                       .consumerARN(consumerArns.get(consumerIndex))
                                                       .startingPosition(s -> s.type(ShardIteratorType.TRIM_HORIZON)),
                                                 responseHandler)
                               .thenAccept(b -> {
                                   if (responseHandler.allEventsReceived && !responseHandler.receivedData.isEmpty()) {
                                       assertThat(producedData).as(responseHandler.id + " has not received all events")
                                                               .containsSequence(responseHandler.receivedData);
                                   }
                               });
                completableFutures.add(completableFuture);
            }
        }
        return completableFutures;
    }

    private void waitForStreamToBeActive() {
        Waiter.run(() -> asyncClient.describeStream(r -> r.streamName(streamName)).join())
              .until(b -> b.streamDescription().streamStatus().equals(StreamStatus.ACTIVE))
              .orFailAfter(Duration.ofMinutes(5));

        Waiter.run(() -> {
                  try {
                      asyncClient.listShards(r -> r.streamName(streamName)).join();
                      return true;
                  } catch (Exception e) {
                      if (e.getCause() instanceof ResourceInUseException) {
                          return false;
                      }
                      throw e;
                  }
              })
              .until(Boolean::booleanValue)
              .orFailAfter(Duration.ofMinutes(1));
    }

    private void waitForConsumersToBeActive() {
        CompletableFuture<?>[] completableFutures =
            consumerArns.stream()
                        .map(a -> CompletableFuture.supplyAsync(
                            () -> Waiter.run(() -> asyncClient.describeStreamConsumer(b -> b.consumerARN(a)).join())
                                        .until(b -> b.consumerDescription().consumerStatus().equals(ConsumerStatus.ACTIVE))
                                        .orFailAfter(Duration.ofMinutes(5)), waiterExecutorService))
                        .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(completableFutures).join();
    }

    private static class TestSubscribeToShardResponseHandler
        extends TestEventStreamingResponseHandler<SubscribeToShardResponse, SubscribeToShardEventStream>
        implements SubscribeToShardResponseHandler {

        private final List<SdkBytes> receivedData = new ArrayList<>();
        private final String id;
        private volatile boolean allEventsReceived = false;

        TestSubscribeToShardResponseHandler(int consumerIndex, int shardIndex) {
            id = "consumer_" + consumerIndex + "_shard_" + shardIndex;
        }

        @Override
        public void onEventStream(SdkPublisher<SubscribeToShardEventStream> publisher) {
            publisher.filter(SubscribeToShardEvent.class)
                     .subscribe(b -> {
                         log.debug(() -> "sequenceNumber " + b.records() + "_" + id);
                         receivedData.addAll(b.records().stream().map(Record::data).collect(Collectors.toList()));
                     });
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            log.error(() -> "Exception from " + id, throwable);
        }

        @Override
        public void complete() {
            allEventsReceived = true;
            log.info(() -> "All events received for " + id);
        }
    }
}
