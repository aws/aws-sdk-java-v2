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

package software.amazon.awssdk.s3benchmarks.s3express;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.Logger;

public class S3PutGetDeleteAsyncBenchmark extends BaseJavaS3ClientBenchmark {
    private static final Logger LOG = Logger.loggerFor(S3PutGetDeleteAsyncBenchmark.class);
    private final S3AsyncClient s3AsyncClient;

    public S3PutGetDeleteAsyncBenchmark(BenchmarkConfig config, S3AsyncClient s3AsyncClient) {
        super(config);
        this.s3AsyncClient = s3AsyncClient;
    }

    @Override
    protected void doRunBenchmark() {
        doRunBenchmark(iteration);
    }

    protected void doRunBenchmark(int iterations) {
        List<CompletableFuture<?>> futures =
            IntStream.range(0, iterations)
                     .mapToObj(this::putObjectFromBytes)
                     .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();

        futures = IntStream.range(0, iterations)
                           .mapToObj(this::getObject)
                           .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();

        futures = IntStream.range(0, iterations)
                           .mapToObj(this::deleteObject)
                           .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
    }

    @Override
    protected void sendOneRequest() {
        try {
            doRunBenchmark(1);
        } catch (S3Exception e) {
            if (e.isThrottlingException()) {
                LOG.debug(() -> "Ignoring throttling exception", e);
            } else {
                throw e;
            }
        }
    }

    private CompletableFuture<?> putObjectFromBytes(int i) {
        return s3AsyncClient.putObject(r -> r.bucket(bucketName(i)).key(key(i)), AsyncRequestBody.fromBytes(contents));
    }

    private CompletableFuture<?> getObject(int i) {
        return s3AsyncClient.getObject(r -> r.bucket(bucketName(i)).key(key(i)), AsyncResponseTransformer.toBytes());
    }

    private CompletableFuture<?> deleteObject(int i) {
        return s3AsyncClient.deleteObject(r -> r.bucket(bucketName(i)).key(key(i)));
    }
}
