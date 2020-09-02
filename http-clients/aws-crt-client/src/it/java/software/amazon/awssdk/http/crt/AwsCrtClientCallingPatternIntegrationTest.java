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

package software.amazon.awssdk.http.crt;

import static software.amazon.awssdk.testutils.service.AwsTestBase.CREDENTIALS_PROVIDER_CHAIN;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.crt.io.HostResolver;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.model.GenerateRandomRequest;
import software.amazon.awssdk.services.kms.model.GenerateRandomResponse;
import software.amazon.awssdk.utils.AttributeMap;


/**
 * Test many possible different calling patterns that users might do, and make sure everything works.
 */
@RunWith(Theories.class)
public class AwsCrtClientCallingPatternIntegrationTest {
    private final static String KEY_ALIAS = "alias/aws-sdk-java-v2-integ-test";
    private final static Region REGION = Region.US_EAST_1;
    private final static int DEFAULT_KEY_SIZE = 32;

    // Success rate will currently never go above ~99% due to aws-c-http not detecting connection close headers, and KMS
    // closing the connection after the 100th Request on a Http Connection.
    // Tracking Issue: https://github.com/awslabs/aws-c-http/issues/106
    private static double MINIMUM_SUCCESS_RATE = 0.95;

    private boolean testWithClient(KmsAsyncClient asyncKMSClient, int numberOfRequests) {
        List<CompletableFuture<GenerateRandomResponse>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfRequests; i++) {
            GenerateRandomRequest request = GenerateRandomRequest.builder().numberOfBytes(DEFAULT_KEY_SIZE).build();
            CompletableFuture<GenerateRandomResponse> future = asyncKMSClient.generateRandom(request);
            futures.add(future);
        }

        List<Exception> failures = new ArrayList<>();
        int actualNumSucceeded = 0;
        for (CompletableFuture<GenerateRandomResponse> f : futures) {
            try {
                GenerateRandomResponse resp = f.get(5, TimeUnit.MINUTES);
                if (200 == resp.sdkHttpResponse().statusCode()) {
                    actualNumSucceeded += 1;
                }
            } catch (Exception e) {
                failures.add(e);
            }
        }

        int minimumNumSucceeded = (int)(numberOfRequests * (MINIMUM_SUCCESS_RATE));
        boolean succeeded = true;
        if (actualNumSucceeded < minimumNumSucceeded) {
            System.err.println("Failure Metrics: numRequests=" + numberOfRequests + ", numSucceeded=" + actualNumSucceeded);
            succeeded = false;
        }

        if (!succeeded) {
            for(Exception e: failures) {
                System.err.println(e.getMessage());
            }
            failures.get(0).printStackTrace();
        }

        return succeeded;
    }

    private boolean testWithNewClient(int eventLoopSize, int numberOfRequests) {

        try (SdkAsyncHttpClient newAwsCrtHttpClient = AwsCrtAsyncHttpClient.builder()
                .build()) {
            try (KmsAsyncClient newAsyncKMSClient = KmsAsyncClient.builder()
                    .region(REGION)
                    .httpClient(newAwsCrtHttpClient)
                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                    .build()) {
                boolean succeeded = testWithClient(newAsyncKMSClient, numberOfRequests);
                return succeeded;
            }
        }
    }

    @DataPoints("EventLoop")
    public static int[] eventLoopValues(){
        return new int[]{1, 4};
    }

    @DataPoints("ConnectionPool")
    public static int[] connectionsValues(){
        /* Don't use 1 connection Pool of size 1, otherwise test takes too long */
        return new int[]{10, 100};
    }

    @DataPoints("NumRequests")
    public static int[] requestValues(){
        return new int[]{1, 25, 250};
    }

    @DataPoints("ParallelClients")
    public static int[] parallelClientValues(){
        return new int[]{1, 2, 8};
    }

    @DataPoints("SharedClient")
    public static boolean[] sharedClientValue(){
        return new boolean[]{true, false};
    }

    @Theory
    public void checkAllCombinations(@FromDataPoints("EventLoop") int eventLoopSize,
                                     @FromDataPoints("ConnectionPool") int connectionPoolSize,
                                     @FromDataPoints("NumRequests") int numberOfRequests,
                                     @FromDataPoints("ParallelClients") int numberOfParallelClients,
                                     @FromDataPoints("SharedClient") boolean useSharedClient) throws Exception {

        try {

            CrtResource.waitForNoResources();
            String testName = String.format("Testing with eventLoopSize %d, connectionPoolSize %d, numberOfRequests %d, " +
                            "numberOfParallelJavaClients %d, useSharedClient %b", eventLoopSize, connectionPoolSize,
                    numberOfRequests, numberOfParallelClients, useSharedClient);
            System.out.println("\n" + testName);

            CountDownLatch latch = new CountDownLatch(numberOfParallelClients);

            AttributeMap attributes = AttributeMap.builder()
                    .put(SdkHttpConfigurationOption.MAX_CONNECTIONS, connectionPoolSize)
                    .build();

            SdkAsyncHttpClient awsCrtHttpClient = AwsCrtAsyncHttpClient.builder()
                    .buildWithDefaults(attributes);

            KmsAsyncClient sharedAsyncKMSClient = KmsAsyncClient.builder()
                    .region(REGION)
                    .httpClient(awsCrtHttpClient)
                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                    .build();

            final AtomicBoolean failed = new AtomicBoolean(false);

            long start = System.currentTimeMillis();
            ExecutorService pool = Executors.newCachedThreadPool();
            for (int threads = 0; threads < numberOfParallelClients; threads++) {
                pool.submit(() -> {
                    if (useSharedClient) {
                        if (!testWithClient(sharedAsyncKMSClient, numberOfRequests)) {
                            System.err.println("Failed: " + testName);
                            failed.set(true);
                        }
                    } else {
                        if (!testWithNewClient(eventLoopSize, numberOfRequests)) {
                            System.err.println("Failed: " + testName);
                            failed.set(true);
                        }
                    }
                    latch.countDown();
                });
            }

            latch.await(5, TimeUnit.MINUTES);

            sharedAsyncKMSClient.close();
            awsCrtHttpClient.close();
            Assert.assertFalse(failed.get());

            CrtResource.waitForNoResources();

            float numSeconds = (float) ((System.currentTimeMillis() - start) / 1000.0);
            String timeElapsed = String.format("%.2f sec", numSeconds);

            System.out.println("Passed: " + testName + ", Time " + timeElapsed);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
