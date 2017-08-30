/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.async.AsyncRequestProvider;
import software.amazon.awssdk.async.AsyncResponseHandler;
import software.amazon.awssdk.async.FileNotSoAsyncResponseHandler;
import software.amazon.awssdk.auth.ProfileCredentialsProvider;
import software.amazon.awssdk.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.async.SimpleSubscriber;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.retry.RetryPolicyAdapter;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class Shorea {

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        S3AsyncClient client = S3AsyncClient
                .builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.builder()
                                                               .profileName("personal")
                                                               .build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                                                                  .retryPolicy(new RetryPolicyAdapter(
                                                                          PredefinedRetryPolicies.NO_RETRY_POLICY))
                                                                  .build())
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        AtomicInteger count = new AtomicInteger(0);

        for (int i = 0; i < 50; i++) {
            executorService.submit(() -> {
                while (true) {
                    client.getObject(GetObjectRequest.builder()
                                                     .bucket("shorea-public")
                                                     .key("empty-object")
                                                     .build(), new AsyncResponseHandler<GetObjectResponse, Void>() {
                        @Override
                        public void responseReceived(GetObjectResponse response) {
                        }

                        @Override
                        public void onStream(Publisher<ByteBuffer> publisher) {
                            publisher.subscribe(new SimpleSubscriber(b -> {
                            }));
                        }

                        @Override
                        public void exceptionOccurred(Throwable throwable) {
                            throwable.printStackTrace();
                        }

                        @Override
                        public Void complete() {
                            return null;
                        }
                    }).join();
                }
            });
        }

        if (true) {
            return;
        }

        client.putObject(PutObjectRequest.builder()
                                         .bucket("shorea-public")
                                         .key("some-key")
                                         .build(),
                         new AsyncRequestProvider() {
                             @Override
                             public long contentLength() {
                                 return 100;
                             }

                             @Override
                             public void subscribe(Subscriber<? super ByteBuffer> s) {
                                 s.onSubscribe(new Subscription() {
                                     @Override
                                     public void request(long n) {
                                         s.onError(new RuntimeException("Failed to produce content"));
                                         //                                         throw new RuntimeException("Failed to produce content");
                                     }

                                     @Override
                                     public void cancel() {

                                     }
                                 });

                             }
                         }
        ).join();

        if (true) {
            return;
        }

        //        AtomicInteger index = new AtomicInteger(1);
        //
        //        int councurrentConnections = 10;
        //        int workerThreadCount = 1;
        //
        //        ExecutorService executorService = Executors.newFixedThreadPool(workerThreadCount);
        //        for (int i = 0; i < workerThreadCount; i++) {
        //            executorService.submit(() -> {
        //                Semaphore permits = new Semaphore(councurrentConnections / workerThreadCount);
        //                while (true) {
        //                    try {
        //                        System.out.println("PUtting object");
        //                        invokeSafely((FunctionalUtils.UnsafeRunnable) permits::acquire);
        //                        //                        client.putObject(PutObjectRequest.builder()
        //                        //                                                         .bucket("shorea-public")
        //                        //                                                         .key("shorea-" + index.incrementAndGet() + ".txt")
        //                        //                                                         .build(),
        //                        //                                         AsyncRequestProvider.fromFile(Paths.get("/tmp/5mb.out")))
        //                        //                              .join();
        //                        getObject(client, index.incrementAndGet()).join();
        //                        System.out.println("Finished put");
        //                    } catch (Exception e) {
        //                        e.printStackTrace();
        //                    } finally {
        //                        permits.release();
        //                    }
        //                }
        //            });
        //        }
    }

    private static CompletableFuture<Void> getObject(S3AsyncClient client, int index) {
        Path downloadPath = Paths.get("/tmp/shorea-" + index + ".txt");
        return client.getObject(GetObjectRequest.builder()
                                                .bucket("shorea-public")
                                                .key("shorea-5.txt")
                                                .build(),
                                new FileNotSoAsyncResponseHandler<>(downloadPath))
                     .thenAccept(r -> invokeSafely(() -> validateMd5(downloadPath, r)));
    }

    private static void validateMd5(Path downloadPath, GetObjectResponse response) throws NoSuchAlgorithmException, IOException {
        //        MessageDigest md = MessageDigest.getInstance("MD5");
        //        try (InputStream is = new DigestInputStream(Files.newInputStream(downloadPath), md)) {
        //            IoUtils.drainInputStream(is);
        //        }
        //        byte[] expectedMd5 = BinaryUtils.fromHex(response.eTag().replace("\"", ""));
        //        byte[] calculatedMd5 = md.digest();
        //        if (!Arrays.equals(expectedMd5, calculatedMd5)) {
        //            throw new RuntimeException(
        //                    String.format("Content malformed. Expected checksum was %s but calculated checksum was %s",
        //                                  BinaryUtils.toBase64(expectedMd5), BinaryUtils.toBase64(calculatedMd5)));
        //        }
    }
}
