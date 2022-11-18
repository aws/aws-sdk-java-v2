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

import static software.amazon.awssdk.s3benchmarks.BenchmarkRunner.BUCKET;
import static software.amazon.awssdk.s3benchmarks.BenchmarkRunner.FILE;
import static software.amazon.awssdk.s3benchmarks.BenchmarkRunner.KEY;
import static software.amazon.awssdk.s3benchmarks.BenchmarkRunner.OPERATION;
import static software.amazon.awssdk.s3benchmarks.BenchmarkRunner.parseConfig;
import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.printOutResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class S3NettyClientBenchmark {
    private final TransferManagerBenchmarkConfig config;
    private final S3AsyncClient s3AsyncClient;
    public S3NettyClientBenchmark(TransferManagerBenchmarkConfig config) {
        this.config = config;
        s3AsyncClient = S3AsyncClient.builder()
            .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                                     .build();
    }

    public static void main(String... args) throws ParseException {

        CommandLineParser parser = new DefaultParser();
        Options options = new Options();

        options.addRequiredOption(null, BUCKET, true, "The s3 bucket");
        options.addOption(null, KEY, true, "The s3 key");
        options.addRequiredOption(null, OPERATION, true, "The operation to run tests: download | upload | copy");
        options.addOption(null, FILE, true, "Destination file path to be written to or source file path to be "
                                            + "uploaded");

        CommandLine cmd = parser.parse(options, args);
        TransferManagerBenchmarkConfig config = parseConfig(cmd);
        S3NettyClientBenchmark s3NettyClientBenchmark = new S3NettyClientBenchmark(config);
        Runnable runnable;

        switch (config.operation()) {
            case DOWNLOAD:
                runnable = s3NettyClientBenchmark::downloadOnce;
                break;
            case UPLOAD:
                runnable = s3NettyClientBenchmark::uploadOnce;
                break;
            case COPY:
                runnable = s3NettyClientBenchmark::copyOnce;
                break;
            default:
                throw new UnsupportedOperationException();
        }

        s3NettyClientBenchmark.run(runnable, config.operation());

    }


    private void copyOnce() {
        s3AsyncClient.copyObject(b -> b.sourceBucket(config.bucket())
                                       .sourceKey(config.key())
                                       .destinationBucket(config.bucket())
                                       .destinationKey(config.key() + "_copy"))
                     .join();
    }


    private void uploadOnce() {
        s3AsyncClient.putObject(b -> b.bucket(config.bucket()).key(config.key()),
                                AsyncRequestBody.fromFile(Paths.get(config.filePath()))).join();
    }

    private void downloadOnce() {
        Path path = Paths.get(config.filePath());
        s3AsyncClient.getObject(b -> b.bucket(config.bucket()).key(config.key()),
                                AsyncResponseTransformer.toFile(path)).join();
        try {
            Files.delete(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run(Runnable runnable, BenchmarkRunner.TransferManagerOperation operation) {
        for (int i = 0; i < 3; i++) {
            runnable.run();
        }

        List<Double> latencies = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            long start = System.currentTimeMillis();
            runnable.run();
            long end = System.currentTimeMillis();
            latencies.add((end - start) / 1000.0);
        }
        printOutResult(latencies, "Netty client " + operation);
    }


}
