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

package software.amazon.awssdk.benchmark.dynamodb;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/**
 * Shared constants for DynamoDB performance benchmark infrastructure (mocked and live).
 * Does not construct clients; factories and benchmarks consume these values.
 */
public final class DynamoDbBenchmarkConstant {

    public static final Region REGION = Region.US_EAST_1;

    public static final String TABLE_NAME = "sdk-java-ddb-perf-benchmark";

    /**
     * Deterministic fake credentials for mocked Tier C clients. Not used for live Tier D.
     */
    public static final AwsCredentialsProvider MOCK_CREDENTIALS_PROVIDER =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

    public static final String MOCK_ERROR_RESPONSE_BODY = "{}";

    private DynamoDbBenchmarkConstant() {
    }
}
