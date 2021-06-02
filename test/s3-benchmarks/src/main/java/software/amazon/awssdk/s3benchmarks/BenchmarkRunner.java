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

import java.util.Locale;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

public class BenchmarkRunner {

    private static final String PART_SIZE_IN_MB = "partSizeInMB";
    private static final String FILE = "file";
    private static final String BUCKET = "bucket";
    private static final String MAX_THROUGHPUT = "maxThroughput";
    private static final String KEY = "key";
    private static final String OPERATION = "operation";

    private BenchmarkRunner() {
    }

    public static void main(String... args) throws org.apache.commons.cli.ParseException {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();

        options.addRequiredOption(null, FILE, true, "Destination file path to be written or source file path to be "
                                                    + "uploaded");
        options.addRequiredOption(null, BUCKET, true, "The s3 bucket");
        options.addRequiredOption(null, KEY, true, "The s3 key");
        options.addRequiredOption(null, OPERATION, true, "The operation to benchmark against");
        options.addOption(null, PART_SIZE_IN_MB, true, "Part size in MB");
        options.addOption(null, MAX_THROUGHPUT, true, "The max throughput");

        CommandLine cmd = parser.parse(options, args);
        TransferManagerBenchmarkConfig config = parseConfig(cmd);
        TransferManagerOperation operation = TransferManagerOperation.valueOf(cmd.getOptionValue(OPERATION)
                                                                                 .toUpperCase(Locale.ENGLISH));
        switch (operation) {
            case DOWNLOAD:
                TransferManagerBenchmark.download(config).run();
                break;
            case UPLOAD:
                TransferManagerBenchmark.upload(config).run();
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private static TransferManagerBenchmarkConfig parseConfig(CommandLine cmd) {
        String filePath = cmd.getOptionValue(FILE);
        String bucket = cmd.getOptionValue(BUCKET);
        String key = cmd.getOptionValue(KEY);

        Long partSize = cmd.getOptionValue(PART_SIZE_IN_MB) == null ? null : Long.parseLong(cmd.getOptionValue(PART_SIZE_IN_MB));

        Double maxThroughput = cmd.getOptionValue(MAX_THROUGHPUT) == null ? null :
                               Double.parseDouble(cmd.getOptionValue(MAX_THROUGHPUT));

        return TransferManagerBenchmarkConfig.builder()
                                             .key(key)
                                             .bucket(bucket)
                                             .partSizeInMb(partSize)
                                             .targetThroughput(maxThroughput)
                                             .filePath(filePath)
                                             .build();
    }

    private enum TransferManagerOperation {
        DOWNLOAD,
        UPLOAD
    }
}
