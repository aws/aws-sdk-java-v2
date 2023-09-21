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

package software.amazon.awssdk.s3benchmarks;

import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.COPY_SUFFIX;

import java.util.List;
import software.amazon.awssdk.utils.Logger;

public class JavaS3ClientCopyBenchmark extends BaseJavaS3ClientBenchmark {
    private static final Logger log = Logger.loggerFor(JavaS3ClientCopyBenchmark.class);

    public JavaS3ClientCopyBenchmark(TransferManagerBenchmarkConfig config) {
        super(config);
    }

    @Override
    protected void sendOneRequest(List<Double> latencies) throws Exception {
        log.info(() -> "Starting copy");
        Double latency = runWithTime(s3AsyncClient.copyObject(
            req -> req.sourceKey(key).sourceBucket(bucket)
                      .destinationBucket(bucket).destinationKey(key + COPY_SUFFIX)
        )::join).latency();
        latencies.add(latency);
    }

    @Override
    protected long contentLength() throws Exception {
        return s3Client.headObject(b -> b.bucket(bucket).key(key)).contentLength();
    }
}
