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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import software.amazon.awssdk.core.FileTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.utils.Logger;

public class JavaS3ClientDownloadBenchmark extends BaseJavaS3ClientBenchmark {
    private static final Logger log = Logger.loggerFor(JavaS3ClientDownloadBenchmark.class);
    private final String filePath;

    public JavaS3ClientDownloadBenchmark(TransferManagerBenchmarkConfig config) {
        super(config);
        this.filePath = config.filePath();
    }

    @Override
    protected void sendOneRequest(List<Double> latencies) throws Exception {
        Double latency;
        if (filePath == null) {
            log.info(() -> "Starting download to memory");
            latency = runWithTime(s3AsyncClient.getObject(
                req -> req.key(key).bucket(bucket), new NoOpResponseTransformer<>()
            )::join).latency();
        } else {
            log.info(() -> "Starting download to file");
            Path path = Paths.get(filePath);
            FileTransformerConfiguration conf = FileTransformerConfiguration
                .builder()
                .failureBehavior(FileTransformerConfiguration.FailureBehavior.LEAVE)
                .fileWriteOption(FileTransformerConfiguration.FileWriteOption.CREATE_OR_REPLACE_EXISTING)
                .build();

            latency = runWithTime(s3AsyncClient.getObject(
                req -> req.key(key).bucket(bucket), AsyncResponseTransformer.toFile(path, conf)
            )::join).latency();
        }
        latencies.add(latency);
    }

    @Override
    protected long contentLength() throws Exception {
        return s3Client.headObject(b -> b.bucket(bucket).key(key)).contentLength();
    }
}
