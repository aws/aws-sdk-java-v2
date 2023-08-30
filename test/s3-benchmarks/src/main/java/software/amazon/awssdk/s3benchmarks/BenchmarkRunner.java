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

import java.time.Duration;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;

public final class BenchmarkRunner {

    private static final String PART_SIZE_IN_MB = "partSizeInMB";
    private static final String FILE = "file";
    private static final String BUCKET = "bucket";
    private static final String MAX_THROUGHPUT = "maxThroughput";
    private static final String KEY = "key";
    private static final String OPERATION = "operation";
    private static final String CHECKSUM_ALGORITHM = "checksumAlgo";
    private static final String ITERATION = "iteration";
    private static final String CONTENT_LENGTH = "contentLengthInMB";

    private static final String READ_BUFFER_IN_MB = "readBufferInMB";

    private static final String VERSION = "version";
    private static final String PREFIX = "prefix";

    private static final String TIMEOUT = "timeoutInMin";

    private static final String CONN_ACQ_TIMEOUT_IN_SEC = "connAcqTimeoutInSec";

    private static final String FORCE_CRT_HTTP_CLIENT = "crtHttp";
    private static final String MAX_CONCURRENCY = "maxConcurrency";

    private static final Map<TransferManagerOperation, Function<TransferManagerBenchmarkConfig, TransferManagerBenchmark>>
        OPERATION_TO_BENCHMARK_V1 = new EnumMap<>(TransferManagerOperation.class);
    private static final Map<TransferManagerOperation, Function<TransferManagerBenchmarkConfig, TransferManagerBenchmark>>
        OPERATION_TO_BENCHMARK_V2 = new EnumMap<>(TransferManagerOperation.class);

    static {
        OPERATION_TO_BENCHMARK_V2.put(TransferManagerOperation.COPY, TransferManagerBenchmark::copy);
        OPERATION_TO_BENCHMARK_V2.put(TransferManagerOperation.DOWNLOAD, TransferManagerBenchmark::v2Download);
        OPERATION_TO_BENCHMARK_V2.put(TransferManagerOperation.UPLOAD, TransferManagerBenchmark::v2Upload);
        OPERATION_TO_BENCHMARK_V2.put(TransferManagerOperation.DOWNLOAD_DIRECTORY, TransferManagerBenchmark::downloadDirectory);
        OPERATION_TO_BENCHMARK_V2.put(TransferManagerOperation.UPLOAD_DIRECTORY, TransferManagerBenchmark::uploadDirectory);

        OPERATION_TO_BENCHMARK_V1.put(TransferManagerOperation.COPY, TransferManagerBenchmark::v1Copy);
        OPERATION_TO_BENCHMARK_V1.put(TransferManagerOperation.DOWNLOAD, TransferManagerBenchmark::v1Download);
        OPERATION_TO_BENCHMARK_V1.put(TransferManagerOperation.UPLOAD, TransferManagerBenchmark::v1Upload);
        OPERATION_TO_BENCHMARK_V1.put(TransferManagerOperation.DOWNLOAD_DIRECTORY, TransferManagerBenchmark::v1DownloadDirectory);
        OPERATION_TO_BENCHMARK_V1.put(TransferManagerOperation.UPLOAD_DIRECTORY, TransferManagerBenchmark::v1UploadDirectory);
    }

    private BenchmarkRunner() {
    }

    public static void main(String... args) throws org.apache.commons.cli.ParseException {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();

        options.addRequiredOption(null, BUCKET, true, "The s3 bucket");
        options.addOption(null, KEY, true, "The s3 key");
        options.addRequiredOption(null, OPERATION, true, "The operation to run tests: download | upload | download_directory | "
                                                         + "upload_directory | copy");
        options.addOption(null, FILE, true, "Destination file path to be written to or source file path to be "
                                            + "uploaded");
        options.addOption(null, PART_SIZE_IN_MB, true, "Part size in MB");
        options.addOption(null, MAX_THROUGHPUT, true, "The max throughput");
        options.addOption(null, CHECKSUM_ALGORITHM, true, "The checksum algorithm to use");
        options.addOption(null, ITERATION, true, "The number of iterations");
        options.addOption(null, READ_BUFFER_IN_MB, true, "Read buffer size in MB");
        options.addOption(null, VERSION, true, "The major version of the transfer manager to run test: "
                                               + "v1 | v2 | crt | java, default: v2");
        options.addOption(null, PREFIX, true, "S3 Prefix used in downloadDirectory and uploadDirectory");

        options.addOption(null, CONTENT_LENGTH, true, "Content length to upload from memory. Used only in the "
                                                      + "CRT Upload Benchmark, but "
                                                      + "is required for this test case.");

        options.addOption(null, TIMEOUT, true, "Amount of minute to wait before a single operation "
                                               + "times out and is cancelled. Optional, defaults to 10 minutes if no specified");
        options.addOption(null, CONN_ACQ_TIMEOUT_IN_SEC, true, "Timeout for acquiring an already-established"
                                                               + " connection from a connection pool to a remote service.");
        options.addOption(null, FORCE_CRT_HTTP_CLIENT, true,
                          "Force the CRT http client to be used in JavaBased benchmarks");
        options.addOption(null, MAX_CONCURRENCY, true,
                          "The Maximum number of allowed concurrent requests. For HTTP/1.1 this is the same as max connections.");

        CommandLine cmd = parser.parse(options, args);
        TransferManagerBenchmarkConfig config = parseConfig(cmd);

        SdkVersion version = SdkVersion.valueOf(cmd.getOptionValue(VERSION, "V2")
                                                   .toUpperCase(Locale.ENGLISH));

        TransferManagerOperation operation = config.operation();
        TransferManagerBenchmark benchmark;

        switch (version) {
            case V1:
                benchmark = OPERATION_TO_BENCHMARK_V1.get(operation).apply(config);
                break;
            case V2:
                benchmark = OPERATION_TO_BENCHMARK_V2.get(operation).apply(config);
                break;
            case CRT:
                if (operation == TransferManagerOperation.DOWNLOAD) {
                    benchmark = new CrtS3ClientDownloadBenchmark(config);
                    break;
                }
                if (operation == TransferManagerOperation.UPLOAD) {
                    benchmark = new CrtS3ClientUploadBenchmark(config);
                    break;
                }
                throw new UnsupportedOperationException();
            case JAVA:
                if (operation == TransferManagerOperation.UPLOAD) {
                    benchmark = new JavaS3ClientUploadBenchmark(config);
                    break;
                }
                if (operation == TransferManagerOperation.COPY) {
                    benchmark = new JavaS3ClientCopyBenchmark(config);
                    break;
                }
                throw new UnsupportedOperationException("Java based s3 client benchmark only support upload and copy");
            default:
                throw new UnsupportedOperationException();
        }
        benchmark.run();
    }

    private static TransferManagerBenchmarkConfig parseConfig(CommandLine cmd) {
        TransferManagerOperation operation = TransferManagerOperation.valueOf(cmd.getOptionValue(OPERATION)
                                                                                 .toUpperCase(Locale.ENGLISH));

        String filePath = cmd.getOptionValue(FILE);
        String bucket = cmd.getOptionValue(BUCKET);
        String key = cmd.getOptionValue(KEY);

        Long partSize = cmd.getOptionValue(PART_SIZE_IN_MB) == null ? null : Long.parseLong(cmd.getOptionValue(PART_SIZE_IN_MB));

        Double maxThroughput = cmd.getOptionValue(MAX_THROUGHPUT) == null ? null :
                               Double.parseDouble(cmd.getOptionValue(MAX_THROUGHPUT));

        ChecksumAlgorithm checksumAlgorithm = null;
        if (cmd.getOptionValue(CHECKSUM_ALGORITHM) != null) {
            checksumAlgorithm = ChecksumAlgorithm.fromValue(cmd.getOptionValue(CHECKSUM_ALGORITHM)
                                                               .toUpperCase(Locale.ENGLISH));
        }

        Integer iteration = cmd.getOptionValue(ITERATION) == null ? null :
                            Integer.parseInt(cmd.getOptionValue(ITERATION));

        Long readBufferInMB = cmd.getOptionValue(READ_BUFFER_IN_MB) == null ? null :
                              Long.parseLong(cmd.getOptionValue(READ_BUFFER_IN_MB));

        String prefix = cmd.getOptionValue(PREFIX);

        Long contentLengthInMb = cmd.getOptionValue(CONTENT_LENGTH) == null ? null :
                                 Long.parseLong(cmd.getOptionValue(CONTENT_LENGTH));

        Duration timeout = cmd.getOptionValue(TIMEOUT) == null ? null :
                           Duration.ofMinutes(Long.parseLong(cmd.getOptionValue(TIMEOUT)));

        Long connAcqTimeoutInSec = cmd.getOptionValue(CONN_ACQ_TIMEOUT_IN_SEC) == null ? null :
                                   Long.parseLong(cmd.getOptionValue(CONN_ACQ_TIMEOUT_IN_SEC));

        Boolean forceCrtHttpClient = cmd.getOptionValue(FORCE_CRT_HTTP_CLIENT) != null
                                     && Boolean.parseBoolean(cmd.getOptionValue(FORCE_CRT_HTTP_CLIENT));

        Integer maxConcurrency = cmd.getOptionValue(MAX_CONCURRENCY) == null ? null :
                                 Integer.parseInt(cmd.getOptionValue(MAX_CONCURRENCY));

        return TransferManagerBenchmarkConfig.builder()
                                             .key(key)
                                             .bucket(bucket)
                                             .partSizeInMb(partSize)
                                             .checksumAlgorithm(checksumAlgorithm)
                                             .targetThroughput(maxThroughput)
                                             .readBufferSizeInMb(readBufferInMB)
                                             .filePath(filePath)
                                             .iteration(iteration)
                                             .operation(operation)
                                             .prefix(prefix)
                                             .contentLengthInMb(contentLengthInMb)
                                             .timeout(timeout)
                                             .connectionAcquisitionTimeoutInSec(connAcqTimeoutInSec)
                                             .forceCrtHttpClient(forceCrtHttpClient)
                                             .maxConcurrency(maxConcurrency)
                                             .build();
    }

    public enum TransferManagerOperation {
        DOWNLOAD,
        UPLOAD,
        COPY,
        DOWNLOAD_DIRECTORY,
        UPLOAD_DIRECTORY
    }

    private enum SdkVersion {
        V1,
        V2,
        CRT,
        JAVA
    }
}
