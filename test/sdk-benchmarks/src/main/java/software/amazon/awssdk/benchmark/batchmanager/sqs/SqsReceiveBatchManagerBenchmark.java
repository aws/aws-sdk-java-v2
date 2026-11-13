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

package software.amazon.awssdk.benchmark.batchmanager.sqs;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * Measures {@link SqsAsyncBatchManager#receiveMessage} throughput against a stub SQS client (see
 * {@link StubSqsAsyncClient}) whose queues always have messages, so the score reflects the batch manager, not the network.
 *
 * <p>{@code queueCount = 1} puts every consumer on one queue. {@code queueCount = 16} spreads consumers across queues;
 * each consumer starts on a different queue and then visits the queues in a fixed round-robin order.
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 5)
@Fork(2)
public class SqsReceiveBatchManagerBenchmark {
    private static final String QUEUE_URL_PREFIX = "https://sqs.us-east-1.amazonaws.com/123456789012/benchmark-queue-";
    private static final int MAX_MESSAGES = 10;
    private static final int SERVICE_THREADS = 4;
    private static final int BATCH_MANAGER_THREADS = 8;

    @Benchmark
    @Threads(1)
    public ReceiveMessageResponse singleConsumer(QueueState queues, ConsumerState consumer) {
        return queues.batchManager.receiveMessage(consumer.nextRequest(queues.requests)).join();
    }

    @Benchmark
    @Threads(16)
    public ReceiveMessageResponse concurrentConsumers(QueueState queues, ConsumerState consumer) {
        return queues.batchManager.receiveMessage(consumer.nextRequest(queues.requests)).join();
    }

    @State(Scope.Benchmark)
    public static class QueueState {
        @Param({"1", "16"})
        private int queueCount;

        /**
         * Delay the stub adds to each SQS call. 0 isolates batch manager overhead; the others model SQS round trips.
         */
        @Param({"0", "2000", "20000", "50000"})
        private long serviceLatencyMicros;

        private final AtomicInteger consumerIds = new AtomicInteger();
        private StubSqsAsyncClient sqs;
        private ScheduledExecutorService batchManagerExecutor;
        private SqsAsyncBatchManager batchManager;
        private ReceiveMessageRequest[] requests;

        @Setup(Level.Trial)
        public void setup() {
            sqs = new StubSqsAsyncClient(serviceLatencyMicros, SERVICE_THREADS);
            batchManagerExecutor = Executors.newScheduledThreadPool(BATCH_MANAGER_THREADS);
            batchManager = SqsAsyncBatchManager.builder()
                                               .client(sqs)
                                               .scheduledExecutor(batchManagerExecutor)
                                               .build();
            requests = new ReceiveMessageRequest[queueCount];
            for (int i = 0; i < queueCount; i++) {
                requests[i] = ReceiveMessageRequest.builder()
                                                   .queueUrl(QUEUE_URL_PREFIX + i)
                                                   .maxNumberOfMessages(MAX_MESSAGES)
                                                   .build();
                // Loads queue attributes and creates the buffer before measurement starts.
                batchManager.receiveMessage(requests[i]).join();
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            batchManager.close();
            batchManagerExecutor.shutdownNow();
            sqs.close();
        }
    }

    @State(Scope.Thread)
    public static class ConsumerState {
        private int next;

        @Setup(Level.Trial)
        public void setup(QueueState queues) {
            next = queues.consumerIds.getAndIncrement() % queues.queueCount;
        }

        ReceiveMessageRequest nextRequest(ReceiveMessageRequest[] requests) {
            ReceiveMessageRequest request = requests[next];
            next = (next + 1) % requests.length;
            return request;
        }
    }

    public static void main(String... args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(SqsReceiveBatchManagerBenchmark.class.getSimpleName())
            .build();
        new Runner(opt).run();
    }
}
