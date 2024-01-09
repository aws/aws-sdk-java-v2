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

package software.amazon.awssdk.services.s3.internal.multipart;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class MultipartDownloadTest {

    private S3AsyncClient s3;
    static final String bucket = "olapplin-test-bucket";
    static final String key = "debug-test-128mb";

    @BeforeEach
    void init() {
        this.s3 = S3AsyncClient.builder()
                               .region(Region.US_WEST_2)
                               .credentialsProvider(ProfileCredentialsProvider.create())
                               .httpClient(NettyNioAsyncHttpClient.create())
                               .build();
    }

    @Test
    void test() {
        AsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> transformer =
            AsyncResponseTransformer.toBytes();

        DownloaderSubscriber downloaderSubscriber = new DownloaderSubscriber(s3, GetObjectRequest.builder()
                                                                                                 .bucket(bucket).key(key)
                                                                                                 .build());

        CompletableFuture<ResponseBytes<GetObjectResponse>> future = new CompletableFuture<>();

        transformer.split(1024 * 1024 * 32, future).subscribe(downloaderSubscriber);
                   // .subscribe(
                   //     new Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>>() {
                   //         private AtomicBoolean totalPartKnown = new AtomicBoolean(false);
                   //         private AtomicInteger totalParts = new AtomicInteger();
                   //         private AtomicInteger completed = new AtomicInteger(0);
                   //         private AtomicInteger currentPart = new AtomicInteger(0);
                   //         private Subscription subscription;
                   //
                   //         @Override
                   //         public void onSubscribe(Subscription s) {
                   //             this.subscription = s;
                   //             s.request(1);
                   //         }
                   //
                   //         @Override
                   //         public void onNext(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> individualTransformer) {
                   //             int part = currentPart.incrementAndGet();
                   //             if (totalPartKnown.get() && part > totalParts.get()) {
                   //                 subscription.cancel();
                   //             }
                   //             System.out.println("[MultipartDownloadTest] trying to send request for part " + part);
                   //             CompletableFuture<GetObjectResponse> response = s3.getObject(GetObjectRequest.builder()
                   //                                                                                          .partNumber(part)
                   //                                                                                          .bucket(bucket)
                   //                                                                                          .key(key)
                   //                                                                                          .build(),
                   //                                                                          individualTransformer);
                   //             response.whenComplete((res, e) -> {
                   //                 System.out.printf("[MultipartDownloadTest] received '%s'%n", res.contentRange());
                   //                 Integer partsCont = res.partsCount();
                   //                 totalParts.set(partsCont);
                   //                 System.out.printf("[MultipartDownloadTest] total parts: %s%n", partsCont);
                   //                 if (partsCont > 1) {
                   //                     subscription.request(1);
                   //                 }
                   //                 if (completed.incrementAndGet() >= totalParts.get()) {
                   //                     subscription.cancel();
                   //                 }
                   //             });
                   //         }
                   //
                   //         @Override
                   //         public void onError(Throwable t) {
                   //             future.completeExceptionally(t);
                   //         }
                   //
                   //         @Override
                   //         public void onComplete() {
                   //             future.complete(new Object());
                   //         }
                   //     });

        future.whenComplete((res, e) -> {
            if (e != null) {
                fail(e);
            }
            System.out.println("[Test] complete");
            System.out.printf("[Test] result: %s%n", res.toString());
            byte[] bytes = res.asByteArray();
            System.out.printf("[Test] Byte len: %s%n", bytes.length);
            System.out.println(new String(bytes));
            System.out.println("[Test] all done");
        });
        future.join();
    }
}
