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

package software.amazon.awssdk.core.internal.async;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.assertj.core.api.Assertions;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.internal.async.ByteArrayAsyncResponseTransformer;
import software.amazon.awssdk.core.internal.async.SplittingPublisherTest;

public final class SplittingPublisherTestUtils {

    public static void verifyIndividualAsyncRequestBody(SdkPublisher<AsyncRequestBody> publisher,
                                                        Path file,
                                                        int chunkSize) throws Exception {

        List<CompletableFuture<byte[]>> futures = new ArrayList<>();
        publisher.subscribe(requestBody -> {
            CompletableFuture<byte[]> baosFuture = new CompletableFuture<>();
            ByteArrayAsyncResponseTransformer.BaosSubscriber subscriber =
                new ByteArrayAsyncResponseTransformer.BaosSubscriber(baosFuture);
            requestBody.subscribe(subscriber);
            futures.add(baosFuture);
        }).get(5, TimeUnit.SECONDS);

        long contentLength = file.toFile().length();
        Assertions.assertThat(futures.size()).isEqualTo((int) Math.ceil(contentLength / (double) chunkSize));

        for (int i = 0; i < futures.size(); i++) {
            try (FileInputStream fileInputStream = new FileInputStream(file.toFile())) {
                byte[] expected;
                if (i == futures.size() - 1) {
                    int lastChunk = contentLength % chunkSize == 0 ? chunkSize : (int) (contentLength % chunkSize);
                    expected = new byte[lastChunk];
                } else {
                    expected = new byte[chunkSize];
                }
                fileInputStream.skip(i * chunkSize);
                fileInputStream.read(expected);
                byte[] actualBytes = futures.get(i).join();
                Assertions.assertThat(actualBytes).isEqualTo(expected);
            }
        }
    }
}
