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

import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.model.ConsumerStatus;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEventStream;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponse;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponseHandler;

public class SubscribeToShardIntegrationTest {

    private static final String STREAM_NAME = "subscribe-to-shard-integ-test-" + System.currentTimeMillis();
    private static final String CONSUMER_NAME = "subscribe-to-shard-consumer";
    private KinesisAsyncClient client;
    private String consumerArn;
    private String shardId;

    @Before
    public void setup() throws InterruptedException {
        client = KinesisAsyncClient.builder()
                                   // TODO credentials and region (whitelisting)
                                   .credentialsProvider(ProfileCredentialsProvider.create("justin-kinesis"))
                                   .region(Region.US_EAST_2)
                                   .build();
        client.createStream(r -> r.streamName(STREAM_NAME)
                                  .shardCount(4)).join();
        waitForStreamToBeActive();
        String streamARN = client.describeStream(r -> r.streamName(STREAM_NAME)).join()
                                 .streamDescription()
                                 .streamARN();
        this.shardId = client.listShards(r -> r.streamName(STREAM_NAME))
                             .join()
                             .shards().get(0).shardId();
        this.consumerArn = client.registerStreamConsumer(r -> r.streamARN(streamARN).consumerName(CONSUMER_NAME)).join()
                                 .consumer()
                                 .consumerARN();
        waitForConsumerToBeActive();
    }

    @After
    public void tearDown() {
        client.deleteStream(r -> r.streamName(STREAM_NAME)
                                  .enforceConsumerDeletion(true)).join();
    }

    @Test
    public void cancelledSubscription_DoesNotCallTerminalMethods() {
        AtomicBoolean terminalCalled = new AtomicBoolean(false);
        try {
            client.subscribeToShard(r -> r.consumerARN(consumerArn)
                                          .shardId(shardId)
                                          .startingPosition(s -> s.type(ShardIteratorType.TRIM_HORIZON)),
                                    new SubscribeToShardResponseHandler() {
                                        @Override
                                        public void responseReceived(SubscribeToShardResponse response) {

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
                                        }

                                        @Override
                                        public void complete() {
                                            terminalCalled.set(true);
                                        }
                                    }).join();
            fail("Expected exception");
        } catch (CompletionException e) {
            assertThat(e.getCause()).hasMessageContaining("cancelled");
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
        waitUntilTrue(() -> StreamStatus.ACTIVE == client.describeStream(r -> r.streamName(STREAM_NAME))
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

}
