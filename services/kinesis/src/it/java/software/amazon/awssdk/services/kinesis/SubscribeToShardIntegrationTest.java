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

package software.amazon.awssdk.services.kinesis;

import static org.assertj.core.api.Assertions.assertThat;

import io.reactivex.Flowable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.nio.netty.Http2Configuration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.kinesis.model.ConsumerStatus;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEvent;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEventStream;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponse;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponseHandler;
import software.amazon.awssdk.testutils.Waiter;

public class SubscribeToShardIntegrationTest extends AbstractTestCase {

    private String streamName;
    private static final String CONSUMER_NAME = "subscribe-to-shard-consumer";
    private static String consumerArn;
    private static String shardId;

    @Before
    public void setup() throws InterruptedException {
        streamName = "subscribe-to-shard-integ-test-" + System.currentTimeMillis();

        asyncClient.createStream(r -> r.streamName(streamName)
                                       .shardCount(1)).join();
        waitForStreamToBeActive();
        String streamARN = asyncClient.describeStream(r -> r.streamName(streamName)).join()
                                      .streamDescription()
                                      .streamARN();

        this.shardId = asyncClient.listShards(r -> r.streamName(streamName))
                                  .join()
                                  .shards().get(0).shardId();
        this.consumerArn = asyncClient.registerStreamConsumer(r -> r.streamARN(streamARN)
                                                                    .consumerName(CONSUMER_NAME)).join()
                                      .consumer()
                                      .consumerARN();
        waitForConsumerToBeActive();
    }

    @After
    public void tearDown() {
        asyncClient.deleteStream(r -> r.streamName(streamName)
                                       .enforceConsumerDeletion(true)).join();
    }

    @Test
    public void subscribeToShard_smallWindow_doesNotTimeOutReads() {
        // We want sufficiently large records (relative to the initial window
        // size we're choosing) so the client has to send multiple
        // WINDOW_UPDATEs to receive them
        for (int i = 0; i < 16; ++i) {
            putRecord(64 * 1024);
        }

        KinesisAsyncClient smallWindowAsyncClient = KinesisAsyncClient.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                    .http2Configuration(Http2Configuration.builder()
                            .initialWindowSize(16384)
                            .build()))
                .build();

        try {
            smallWindowAsyncClient.subscribeToShard(r -> r.consumerARN(consumerArn)
                            .shardId(shardId)
                            .startingPosition(s -> s.type(ShardIteratorType.TRIM_HORIZON)),
                    SubscribeToShardResponseHandler.builder()
                            .onEventStream(es -> Flowable.fromPublisher(es).forEach(e -> {}))
                            .onResponse(this::verifyHttpMetadata)
                            .build())
                    .join();

        } finally {
            smallWindowAsyncClient.close();
        }
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
        asyncClient.subscribeToShard(r -> r.consumerARN(consumerArn)
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
    public void cancelledSubscription_doesNotCallTerminalMethods() {
        AtomicBoolean terminalMethodsCalled = new AtomicBoolean(false);
        AtomicBoolean errorOccurred = new AtomicBoolean(false);
        List<SubscribeToShardEventStream> events = new ArrayList<>();
        asyncClient.subscribeToShard(r -> r.consumerARN(consumerArn)
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
                                                     events.add(subscribeToShardEventStream);
                                                 }

                                                 @Override
                                                 public void onError(Throwable throwable) {
                                                     errorOccurred.set(true);
                                                 }

                                                 @Override
                                                 public void onComplete() {
                                                     terminalMethodsCalled.set(true);
                                                 }
                                             });
                                         }

                                         @Override
                                         public void exceptionOccurred(Throwable throwable) {
                                             errorOccurred.set(true);
                                         }

                                         @Override
                                         public void complete() {
                                             terminalMethodsCalled.set(true);
                                         }
                                     }).join();

        assertThat(terminalMethodsCalled).isFalse();
        assertThat(errorOccurred).isFalse();
        assertThat(events.size()).isEqualTo(3);

    }

    private static void waitForConsumerToBeActive() {
        Waiter.run(() -> asyncClient.describeStreamConsumer(r -> r.consumerARN(consumerArn)).join())
              .until(b -> b.consumerDescription().consumerStatus().equals(ConsumerStatus.ACTIVE))
              .orFailAfter(Duration.ofMinutes(5));
    }

    private void waitForStreamToBeActive() {
        Waiter.run(() -> asyncClient.describeStream(r -> r.streamName(streamName)).join())
              .until(b -> b.streamDescription().streamStatus().equals(StreamStatus.ACTIVE))
              .orFailAfter(Duration.ofMinutes(5));
    }


    /**
     * Puts a random record to the stream.
     *
     * @return Record data that was put.
     */
    private Optional<SdkBytes> putRecord() {
        return putRecord(50);
    }

    /**
     * Puts a random record to the stream.
     *
     * @param len The number of bytes to generate for the record.
     * @return Record data that was put.
     */
    private Optional<SdkBytes> putRecord(int len) {
        try {
            SdkBytes data = SdkBytes.fromByteArray(RandomUtils.nextBytes(len));
            asyncClient.putRecord(PutRecordRequest.builder()
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
        assertThat(response.responseMetadata()).isNotNull();
        assertThat(response.responseMetadata().extendedRequestId()).isNotEqualTo("UNKNOWN");
        assertThat(response.responseMetadata().requestId()).isNotEqualTo("UNKNOWN");
    }
}
