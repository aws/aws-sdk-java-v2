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

import java.util.stream.IntStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.Logger;

public class S3PutGetDeleteSyncBenchmark extends BaseJavaS3ClientBenchmark {
    private static final Logger LOG = Logger.loggerFor(S3PutGetDeleteSyncBenchmark.class);
    private final S3Client s3Client;

    public S3PutGetDeleteSyncBenchmark(BenchmarkConfig config, S3Client s3Client) {
        super(config);
        this.s3Client = s3Client;
    }

    @Override
    protected void doRunBenchmark() {
        IntStream.range(0, benchmarkConfig.iteration()).forEach(this::putGetDeleteObjectFromBytes);
    }

    @Override
    protected void sendOneRequest() {
        try {
            putGetDeleteObjectFromBytes(1);
        } catch (S3Exception e) {
            if (e.isThrottlingException()) {
                LOG.debug(() -> "Ignoring throttling exception", e);
            } else {
                throw e;
            }
        }
    }

    private void putGetDeleteObjectFromBytes(int i) {
        s3Client.putObject(r -> r.bucket(bucketName(i)).key(key(i)), RequestBody.fromBytes(contents));
        s3Client.getObject(r -> r.bucket(bucketName(i)).key(key(i)), ResponseTransformer.toBytes());
        s3Client.deleteObject(r -> r.bucket(bucketName(i)).key(key(i)));
    }
}
