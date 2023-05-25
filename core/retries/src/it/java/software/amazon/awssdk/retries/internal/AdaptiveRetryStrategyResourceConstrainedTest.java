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

package software.amazon.awssdk.retries.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RecordSuccessRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;
import software.amazon.awssdk.retries.internal.ratelimiter.RateLimiterTokenBucketStore;

/**
 * Test for the AdaptiveRetryStrategy correctness and thread safety. This test creates a producer consumer scenario with a limited
 * set of consumer workers (server) and a larger amount of producers (client) that create random pairs of matrices of fixed size
 * for the server to multiply them. This scenario simulates the expected use of the adaptive retry strategy where all the calls
 * are made to a very resource-constrained set of resources, and we expect that the delays created by the rate limiter allows the
 * perceived availability for the clients to be close to 1.0.
 */
class AdaptiveRetryStrategyResourceConstrainedTest {
    static final int DEFAULT_EXCEPTION_TOKEN_COST = 5;

    @Test
    void seemsToBeCorrectAndThreadSafe() {
        // Arrange the test. We allocate a single thread for each server worker
        // and for each client worker.
        int serverWorkers = 1;
        int clientWorkers = 5;
        int parallelism = serverWorkers + clientWorkers;
        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        Server server = new Server(serverWorkers, executor);
        AdaptiveRetryStrategy strategy = DefaultAdaptiveRetryStrategy2
            .builder()
            // We don't care about how many attempts we allow to, that logic is tested somewhere else.
            // so we give the strategy plenty of room for retries.
            .maxAttempts(20)
            .tokenBucketExceptionCost(DEFAULT_EXCEPTION_TOKEN_COST)
            .tokenBucketStore(TokenBucketStore.builder().tokenBucketMaxCapacity(10_000).build())
            // Just wait for the rate limiter delays.
            .backoffStrategy(BackoffStrategy.retryImmediately())
            .rateLimiterTokenBucketStore(RateLimiterTokenBucketStore.builder().build())
            .retryOnExceptionInstanceOf(ThrottlingException.class)
            .treatAsThrottling(x -> x instanceof ThrottlingException)
            .build();
        List<Client> clients = createClients(server, strategy, clientWorkers, 8);

        // Start the clients and wait for all of them to complete.
        CompletableFuture.allOf(clients.stream()
                                       .map(client -> CompletableFuture.runAsync(client::processAllJobs, executor))
                                       .toArray(CompletableFuture[]::new))
                         .join();

        server.stop();
        // Assert here that the average of the perceived availability, that is the (number of jobs / attempts)  is close to
        // 1.0 within 20%.
        double total = clients.stream().mapToDouble(Client::perceivedAvailability).sum();
        double avg = total / clients.size();

        assertThat(avg).isCloseTo(1.0, withinPercentage(20));
        executor.shutdown();
    }

    private static List<Client> createClients(Server server, AdaptiveRetryStrategy strategy, int amount, int jobsPerClient) {
        return IntStream.range(0, amount)
                        .mapToObj(idx -> createClient(server, strategy, jobsPerClient))
                        .collect(Collectors.toCollection(() -> new ArrayList<>(amount)));
    }

    private static Client createClient(Server server, AdaptiveRetryStrategy strategy, int jobs) {
        return new Client(createJobs(jobs), server, strategy);
    }

    private static List<Job> createJobs(int amount) {
        // We use a non-small but fixed size here instead of random ones to have a more predictable workload.
        int rows = 256;
        int cols = 256 + 128;
        return IntStream.range(0, amount)
                        .mapToObj(idx -> createRandomJob(rows, cols))
                        .collect(Collectors.toCollection(() -> new ArrayList<>(amount)));
    }

    private static Job createRandomJob(int rows, int cols) {
        double[][] left = generateMatrix(rows, cols);
        double[][] right = generateMatrix(cols, rows);
        return new Job(left, right);
    }

    public static double[][] generateMatrix(int rows, int cols) {
        int bias = 16777619;
        Random rand = ThreadLocalRandom.current();
        double[][] matrix = new double[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                matrix[row][col] = bias * rand.nextDouble();
            }
        }
        return matrix;
    }

    static class Client {
        private final List<Job> jobs;
        private final Server server;
        private final AdaptiveRetryStrategy strategy;
        private int attempts = 0;

        Client(List<Job> jobs, Server server, AdaptiveRetryStrategy strategy) {
            this.jobs = jobs;
            this.server = server;
            this.strategy = strategy;
        }

        void processAllJobs() {
            for (Job job : jobs) {
                process(job);
            }
        }

        void process(Job job) {
            // submit job
            AcquireInitialTokenResponse response = strategy.acquireInitialToken(AcquireInitialTokenRequest.create("client"));
            RetryToken token = response.token();
            sleep(response.delay());
            do {
                try {
                    ++attempts;
                    server.accept(job);
                    break;
                } catch (Throwable e) {
                    RefreshRetryTokenResponse refreshResponse =
                        strategy.refreshRetryToken(RefreshRetryTokenRequest.builder()
                                                                           .token(token)
                                                                           .failure(e)
                                                                           .build());
                    token = refreshResponse.token();
                    sleep(refreshResponse.delay());
                }
            } while (true);

            // Block until the job is completed.
            synchronized (job.guard) {
                while (true) {
                    try {
                        job.guard.wait(5);
                    } catch (InterruptedException ignored) {
                    }
                    if (job.isDone()) {
                        strategy.recordSuccess(RecordSuccessRequest.create(token));
                        break;
                    }
                }
            }
        }

        void sleep(Duration duration) {
            if (!duration.isZero()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(duration.toMillis());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        double perceivedAvailability() {
            return jobs.size() / (double) attempts;
        }
    }

    static class Server {
        private static final Job POISON_PILL = new Job(new double[0][0], new double[0][0]);
        private final BlockingQueue<Job> jobQueue;
        private final List<CompletableFuture<Void>> workers;

        Server(int totalWorkers, ExecutorService executor) {
            this.jobQueue = new ArrayBlockingQueue<>(totalWorkers * 2);
            this.workers = IntStream.range(0, totalWorkers)
                                    .mapToObj(idx -> CompletableFuture.runAsync(new ServerWorker(jobQueue), executor))
                                    .collect(Collectors.toCollection(() -> new ArrayList<>(totalWorkers)));
        }

        void stop() {
            try {
                for (int idx = 0; idx < workers.size(); idx++) {
                    jobQueue.put(POISON_PILL);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            CompletableFuture.allOf(workers.toArray(new CompletableFuture[0])).join();
        }

        void accept(Job job) {
            if (!jobQueue.offer(job)) {
                // No space left in the queue to take this job, throw a ThrottlingException to notify the
                // client about it and let him retry at a later time.
                throw new ThrottlingException();
            }
        }
    }

    static class ServerWorker implements Runnable {
        private final BlockingQueue<Job> jobQueue;

        ServerWorker(BlockingQueue<Job> jobQueue) {
            this.jobQueue = jobQueue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Job job = jobQueue.poll(10, TimeUnit.MILLISECONDS);
                    if (job == Server.POISON_PILL) {
                        // Break from the loop, this signals that the work is done.
                        break;
                    }
                    if (job != null) {
                        synchronized (job.guard) {
                            // Process the request and notify the client when the result is ready.
                            job.setResult(multiplyMatrices(job.left, job.right));
                            job.guard.notifyAll();
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // Actual logic. We use a matrix multiplication instead of sleeping for a random
        // amount of time to get some predictable workload.
        static double[][] multiplyMatrices(double[][] firstMatrix, double[][] secondMatrix) {
            double[][] result = new double[firstMatrix.length][secondMatrix[0].length];
            for (int row = 0; row < result.length; row++) {
                for (int col = 0; col < result[row].length; col++) {
                    result[row][col] = multiplyMatricesCell(firstMatrix, secondMatrix, row, col);
                }
            }
            return result;
        }

        static double multiplyMatricesCell(double[][] firstMatrix, double[][] secondMatrix, int row, int col) {
            return IntStream.range(0, secondMatrix.length).mapToDouble(i -> firstMatrix[row][i] * secondMatrix[i][col]).sum();
        }
    }

    static class Job {
        final Object guard = new Object();
        final double[][] left;
        final double[][] right;
        double[][] result;

        Job(double[][] left, double[][] right) {
            this.left = left;
            this.right = right;
            this.result = null;
        }

        boolean isDone() {
            return result != null;
        }

        void setResult(double[][] result) {
            this.result = result;
        }
    }

    static class ThrottlingException extends RuntimeException {
    }
}
