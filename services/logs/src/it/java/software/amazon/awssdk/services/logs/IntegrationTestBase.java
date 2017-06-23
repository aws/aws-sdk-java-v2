/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.logs;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeMetricFiltersRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeMetricFiltersResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogGroup;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogStream;
import software.amazon.awssdk.services.cloudwatchlogs.model.MetricFilter;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

/**
 * Base class for CloudWatch Logs integration tests.
 */
public abstract class IntegrationTestBase extends AwsIntegrationTestBase {

    /** Shared CloudWatch Logs client for all tests to use. */
    protected static CloudWatchLogsClient awsLogs;

    /**
     * Loads the AWS account info for the integration tests and creates an CloudWatch Logs client
     * for tests to use.
     */
    @BeforeClass
    public static void setupFixture() throws FileNotFoundException, IOException {
        awsLogs = CloudWatchLogsClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    /*
     * Test helper functions
     */

    /**
     * @return The LogGroup object included in the DescribeLogGroups response, or null if such group
     *         is not found.
     */
    protected static LogGroup findLogGroupByName(final CloudWatchLogsClient awsLogs, final String groupName) {
        String nextToken = null;

        do {
            DescribeLogGroupsResponse result = awsLogs
                    .describeLogGroups(DescribeLogGroupsRequest.builder().nextToken(nextToken).build());

            for (LogGroup group : result.logGroups()) {
                if (group.logGroupName().equals(groupName)) {
                    return group;
                }
            }
            nextToken = result.nextToken();
        } while (nextToken != null);

        return null;
    }

    /**
     * @return The LogStream object included in the DescribeLogStreams response, or null if such
     *         stream is not found in the specified group.
     */
    protected static LogStream findLogStreamByName(final CloudWatchLogsClient awsLogs,
                                                   final String logGroupName,
                                                   final String logStreamName) {
        String nextToken = null;

        do {
            DescribeLogStreamsResponse result = awsLogs
                    .describeLogStreams(DescribeLogStreamsRequest.builder()
                                                                 .logGroupName(logGroupName)
                                                                 .nextToken(nextToken)
                                                                 .build());

            for (LogStream stream : result.logStreams()) {
                if (stream.logStreamName().equals(logStreamName)) {
                    return stream;
                }
            }
            nextToken = result.nextToken();
        } while (nextToken != null);

        return null;
    }

    /**
     * @return The MetricFilter object included in the DescribeMetricFilters response, or null if
     *         such filter is not found in the specified group.
     */
    protected static MetricFilter findMetricFilterByName(final CloudWatchLogsClient awsLogs,
                                                         final String logGroupName,
                                                         final String filterName) {
        String nextToken = null;

        do {
            DescribeMetricFiltersResponse result = awsLogs
                    .describeMetricFilters(DescribeMetricFiltersRequest.builder()
                                                                       .logGroupName(logGroupName)
                                                                       .nextToken(nextToken)
                                                                       .build());

            for (MetricFilter mf : result.metricFilters()) {
                if (mf.filterName().equals(filterName)) {
                    return mf;
                }
            }
            nextToken = result.nextToken();
        } while (nextToken != null);

        return null;
    }
}
