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

package software.amazon.awssdk.services.kinesis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.http.SdkCancellationException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.model.ConsumerStatus;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEvent;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEventStream;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponse;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponseHandler;

public class SubscribeToShardIntegrationTest {

    private String streamName;
    private static final String CONSUMER_NAME = "subscribe-to-shard-consumer";
    private KinesisAsyncClient client;
    private String consumerArn;
    private String shardId;

    @Before
    public void setup() throws InterruptedException {
        streamName = "subscribe-to-shard-integ-test-" + System.currentTimeMillis();
        client = KinesisAsyncClient.builder()
                                   .region(Region.EU_CENTRAL_1)
                                   .build();
        client.createStream(r -> r.streamName(streamName)
                                  .shardCount(1)).join();
        waitForStreamToBeActive();
        String streamARN = client.describeStream(r -> r.streamName(streamName)).join()
                                 .streamDescription()
                                 .streamARN();
        this.shardId = client.listShards(r -> r.streamName(streamName))
                             .join()
                             .shards().get(0).shardId();
        this.consumerArn = client.registerStreamConsumer(r -> r.streamARN(streamARN)
                                                               .consumerName(CONSUMER_NAME)).join()
                                 .consumer()
                                 .consumerARN();
        waitForConsumerToBeActive();
    }

    @After
    public void tearDown() {
        client.deleteStream(r -> r.streamName(streamName)
                                  .enforceConsumerDeletion(true)).join();
    }

    @Test
    public void subscribeToShard_ReceivesAllData() {
        List<SdkBytes> producedData = new ArrayList<>();
        ScheduledExecutorService producer = Executors.newScheduledThreadPool(1);
        // Delay it a bit to allow us to subscribe first
        producer.scheduleAtFixedRate(() -> putRecord().ifPresent(producedData::add), 10, 1, TimeUnit.SECONDS);

        List<SdkBytes> receivedData = new ArrayList<>();
        // Add every event's data to the receivedData list
        Consumer<SubscribeToShardEvent> eventConsumer = s -> receivedData.addAll(
            s.records().stream()
             .map(Record::data)
             .collect(Collectors.toList()));
        client.subscribeToShard(r -> r.consumerARN(consumerArn)
                                      .shardId(shardId)
                                      .startingPosition(s -> s.type(ShardIteratorType.LATEST)),
                                SubscribeToShardResponseHandler.builder()
                                                               .onEventStream(p -> p.filter(SubscribeToShardEvent.class)
                                                                                    .subscribe(eventConsumer))
                                                               .onResponse(this::verifyHttpMetadata)
                                                               .build())
              .join();
        producer.shutdown();
        // Make sure we all the data we received was data we published, we may have published more
        // if the producer isn't shutdown immediately after we finish subscribing.
        assertThat(producedData).containsSequence(receivedData);
    }

    @Test
    public void cancelledSubscription_DoesNotCallTerminalMethods() {
        AtomicBoolean terminalCalled = new AtomicBoolean(false);
        AtomicReference<Throwable> exceptionOccurredThrowable = new AtomicReference<>();
        try {
            client.subscribeToShard(r -> r.consumerARN(consumerArn)
                                          .shardId(shardId)
                                          .startingPosition(s -> s.type(ShardIteratorType.LATEST)),
                                    new SubscribeToShardResponseHandler() {
                                        @Override
                                        public void responseReceived(SubscribeToShardResponse response) {
                                            verifyHttpMetadata(response);
                                        }

                                        @Override
                                        public void onEventStream(SdkPublisher<SubscribeToShardEventStream> publisher) {
                                            publisher.limit(3).subscribe(new Subscriber<SubscribeToShardEventStream>() {
                                                @Override
                                                public void onSubscribe(Subscription subscription) {
                                                    subscription.request(10);
                                                }

                                                @Override
                                                public void onNext(SubscribeToShardEventStream subscribeToShardEventStream) {
                                                }

                                                @Override
                                                public void onError(Throwable throwable) {
                                                    terminalCalled.set(true);
                                                }

                                                @Override
                                                public void onComplete() {
                                                    terminalCalled.set(true);
                                                }
                                            });
                                        }

                                        @Override
                                        public void exceptionOccurred(Throwable throwable) {
                                            // Expected to be called
                                            exceptionOccurredThrowable.set(throwable);
                                        }

                                        @Override
                                        public void complete() {
                                            terminalCalled.set(true);
                                        }
                                    }).join();
            fail("Expected exception");
        } catch (CompletionException e) {
            assertThat(e.getCause().getCause()).isInstanceOf(SdkCancellationException.class);
            assertThat(exceptionOccurredThrowable.get().getCause().getCause()).isInstanceOf(SdkCancellationException.class);
            assertThat(terminalCalled).as("complete or onComplete was called when it shouldn't have been")
                                      .isFalse();
        }
    }

    private void waitForConsumerToBeActive() throws InterruptedException {
        waitUntilTrue(() -> ConsumerStatus.ACTIVE == client.describeStreamConsumer(r -> r.consumerARN(consumerArn))
                                                           .join()
                                                           .consumerDescription()
                                                           .consumerStatus());
    }

    private void waitForStreamToBeActive() throws InterruptedException {
        waitUntilTrue(() -> StreamStatus.ACTIVE == client.describeStream(r -> r.streamName(streamName))
                                                         .join()
                                                         .streamDescription()
                                                         .streamStatus());
    }

    private void waitUntilTrue(Supplier<Boolean> state) throws InterruptedException {
        int attempt = 0;
        do {
            if (attempt > 10) {
                throw new IllegalStateException("State never transitioned");
            }
            Thread.sleep(5000);
            attempt++;
            if (state.get()) {
                return;
            }
        } while (true);
    }

    /**
     * Puts a random record to the stream.
     *
     * @return Record data that was put.
     */
    private Optional<SdkBytes> putRecord() {
        try {
            SdkBytes data = SdkBytes.fromByteArray(RandomUtils.nextBytes(50));
            client.putRecord(PutRecordRequest.builder()
                                             .streamName(streamName)
                                             .data(data)
                                             .partitionKey(UUID.randomUUID().toString())
                                             .build())
                  .join();
            return Optional.of(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private void verifyHttpMetadata(SubscribeToShardResponse response) {
        SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
        assertThat(sdkHttpResponse).isNotNull();
        assertThat(sdkHttpResponse.isSuccessful()).isTrue();
        assertThat(sdkHttpResponse.headers()).isNotEmpty();
    }
}