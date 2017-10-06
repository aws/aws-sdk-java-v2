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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.AmazonServiceException;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DeleteLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DeleteLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DeleteMetricFilterRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DeleteRetentionPolicyRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.InvalidSequenceTokenException;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogGroup;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogStream;
import software.amazon.awssdk.services.cloudwatchlogs.model.MetricFilter;
import software.amazon.awssdk.services.cloudwatchlogs.model.MetricFilterMatchRecord;
import software.amazon.awssdk.services.cloudwatchlogs.model.MetricTransformation;
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutMetricFilterRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutRetentionPolicyRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.cloudwatchlogs.model.TestMetricFilterRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.TestMetricFilterResponse;

/**
 * Integration tests for the CloudWatch Logs service.
 */
public class ServiceIntegrationTest extends IntegrationTestBase {

    /* Components of the message body. */
    private static final long LOG_MESSAGE_TIMESTAMP = System.currentTimeMillis();
    private static final String LOG_MESSAGE_PREFIX = "java-integ-test";
    private static final String LOG_MESSAGE_CONTENT = "boom";

    /* The log message body and the pattern that we use to filter out such message. */
    private static final String LOG_MESSAGE = String.format("%s [%d] %s", LOG_MESSAGE_PREFIX, LOG_MESSAGE_TIMESTAMP,
                                                            LOG_MESSAGE_CONTENT);
    private static final String LOG_METRIC_FILTER_PATTERN = "[prefix=java-integ-test, timestamp, content]";

    private static final String CLOUDWATCH_METRIC_NAME = "java-integ-test-transformed-metric-name";
    private static final String CLOUDWATCH_METRIC_NAMESPACE = "java-integ-test-transformed-metric-namespace";

    private static final int LOG_RETENTION_DAYS = 5;

    private final String nameSuffix = String.valueOf(System.currentTimeMillis());
    private final String logGroupName = "java-integ-test-log-group-name-" + nameSuffix;
    private final String logStreamName = "java-integ-test-log-stream-name-" + nameSuffix;
    private final String logMetricFilterName = "java-integ-test-log-metric-filter-" + nameSuffix;

    /**
     * Test creating a log group using the specified group name.
     */
    public static void testCreateLogGroup(final String groupName) {
        awsLogs.createLogGroup(CreateLogGroupRequest.builder().logGroupName(groupName).build());

        try {
            awsLogs.createLogGroup(CreateLogGroupRequest.builder().logGroupName(groupName).build());
            Assert.fail("ResourceAlreadyExistsException is expected.");
        } catch (ResourceAlreadyExistsException expected) {
            // Ignored or expected.
        }

        final LogGroup createdGroup = findLogGroupByName(awsLogs, groupName);

        Assert.assertNotNull(String.format("Log group [%s] is not found in the DescribeLogGroups response.", groupName),
                             createdGroup);

        Assert.assertEquals(groupName, createdGroup.logGroupName());
        Assert.assertNotNull(createdGroup.creationTime());
        Assert.assertNotNull(createdGroup.arn());

        /* The log group should have no filter and no stored bytes. */
        Assert.assertEquals(0, createdGroup.metricFilterCount().intValue());
        Assert.assertEquals(0, createdGroup.storedBytes().longValue());

        /* Retention policy is still unspecified. */
        Assert.assertNull(createdGroup.retentionInDays());

    }

    /**
     * Test creating a log stream for the specified group.
     */
    public static void testCreateLogStream(final String groupName, final String logStreamName) {
        awsLogs.createLogStream(CreateLogStreamRequest.builder().logGroupName(groupName).logStreamName(logStreamName).build());

        try {
            awsLogs.createLogStream(CreateLogStreamRequest.builder().logGroupName(groupName).logStreamName(logStreamName).build());
            Assert.fail("ResourceAlreadyExistsException is expected.");
        } catch (ResourceAlreadyExistsException expected) {
            // Ignored or expected.
        }

        final LogStream createdStream = findLogStreamByName(awsLogs, groupName, logStreamName);

        Assert.assertNotNull(
                String.format("Log stream [%s] is not found in the [%s] log group.", logStreamName, groupName),
                createdStream);

        Assert.assertEquals(logStreamName, createdStream.logStreamName());
        Assert.assertNotNull(createdStream.creationTime());
        Assert.assertNotNull(createdStream.arn());

        /* The log stream should have no stored bytes. */
        Assert.assertEquals(0, createdStream.storedBytes().longValue());

        /* No log event is pushed yet. */
        Assert.assertNull(createdStream.firstEventTimestamp());
        Assert.assertNull(createdStream.lastEventTimestamp());
        Assert.assertNull(createdStream.lastIngestionTime());
    }

    @Before
    public void setup() throws IOException {
        testCreateLogGroup(logGroupName);
        testCreateLogStream(logGroupName, logStreamName);
        testCreateMetricFilter(logGroupName, logMetricFilterName);
    }

    @After
    public void tearDown() {
        try {
            awsLogs.deleteLogStream(DeleteLogStreamRequest.builder().logGroupName(logGroupName).logStreamName(logStreamName).build());
        } catch (AmazonServiceException ase) {
            System.err.println("Unable to delete log stream " + logStreamName);
        }
        try {
            awsLogs.deleteMetricFilter(DeleteMetricFilterRequest.builder().logGroupName(logGroupName).filterName(logMetricFilterName).build());
        } catch (AmazonServiceException ase) {
            System.err.println("Unable to delete metric filter " + logMetricFilterName);
        }
        try {
            awsLogs.deleteLogGroup(DeleteLogGroupRequest.builder().logGroupName(logGroupName).build());
        } catch (AmazonServiceException ase) {
            System.err.println("Unable to delete log group " + logGroupName);
        }
    }

    /**
     * Test uploading and retrieving log events.
     */
    @Test
    public void testEventsLogging() {
        // No log event is expected in the newly created log stream
        GetLogEventsResponse getResult = awsLogs.getLogEvents(GetLogEventsRequest.builder().logGroupName(logGroupName).logStreamName(logStreamName).build());
        Assert.assertTrue(getResult.events().isEmpty());

        // Insert a new log event
        PutLogEventsRequest request = PutLogEventsRequest.builder()
                                                         .logGroupName(logGroupName)
                                                         .logStreamName(logStreamName)
                                                         .logEvents(InputLogEvent.builder()
                                                                                 .message(LOG_MESSAGE)
                                                                                 .timestamp(LOG_MESSAGE_TIMESTAMP)
                                                                                 .build())
                                                         .build();
        PutLogEventsResponse putResult = awsLogs.putLogEvents(request);

        Assert.assertNotNull(putResult.nextSequenceToken());

        // The new log event is not instantly available in GetLogEvents operation.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
            // Ignored or expected.
        }

        // Pull the event from the log stream
        getResult = awsLogs.getLogEvents(GetLogEventsRequest.builder().logGroupName(logGroupName).logStreamName(logStreamName).build());
        Assert.assertEquals(1, getResult.events().size());
        Assert.assertNotNull(getResult.nextBackwardToken());
        Assert.assertNotNull(getResult.nextForwardToken());

        OutputLogEvent event = getResult.events().get(0);
        Assert.assertEquals(LOG_MESSAGE, event.message());
        Assert.assertEquals(LOG_MESSAGE_TIMESTAMP, event.timestamp().longValue());

        // Use DescribeLogStreams API to verify that the new log event has
        // updated the following parameters of the log stream.
        final LogStream stream = findLogStreamByName(awsLogs, logGroupName, logStreamName);
        Assert.assertEquals(LOG_MESSAGE_TIMESTAMP, stream.firstEventTimestamp().longValue());
        Assert.assertEquals(LOG_MESSAGE_TIMESTAMP, stream.lastEventTimestamp().longValue());
        Assert.assertNotNull(stream.lastIngestionTime());
    }

    /**
     * Use the TestMetricFilter API to verify the correctness of the metric filter pattern we have
     * been using in this integration test.
     */
    @Test
    public void testMetricFilter() {
        TestMetricFilterRequest request =
                TestMetricFilterRequest.builder()
                                       .filterPattern(LOG_METRIC_FILTER_PATTERN)
                                       .logEventMessages(LOG_MESSAGE,
                                                         "Another message with some content that does not match the filter pattern...")
                                       .build();
        TestMetricFilterResponse testResult = awsLogs.testMetricFilter(request);

        Assert.assertEquals(1, testResult.matches().size());

        MetricFilterMatchRecord match = testResult.matches().get(0);
        // Event numbers starts from 1
        Assert.assertEquals(1, match.eventNumber().longValue());
        Assert.assertEquals(LOG_MESSAGE, match.eventMessage());

        // Verify the extracted values
        Map<String, String> extractedValues = match.extractedValues();
        Assert.assertEquals(3, extractedValues.size());
        Assert.assertEquals(LOG_MESSAGE_PREFIX, extractedValues.get("$prefix"));
        Assert.assertEquals(LOG_MESSAGE_TIMESTAMP, Long.parseLong(extractedValues.get("$timestamp")));
        Assert.assertEquals(LOG_MESSAGE_CONTENT, extractedValues.get("$content"));
    }

    /**
     * Tests that we have deserialized the exception response correctly. See TT0064111680
     */
    @Test
    public void putLogEvents_InvalidSequenceNumber_HasExpectedSequenceNumberInException() {
        // First call to PutLogEvents does not need a sequence number, subsequent calls do
        awsLogs.putLogEvents(PutLogEventsRequest.builder()
                                                .logGroupName(logGroupName)
                                                .logStreamName(logStreamName)
                                                .logEvents(InputLogEvent.builder().message(LOG_MESSAGE).timestamp(LOG_MESSAGE_TIMESTAMP).build())
                                                .build());
        try {
            // This call requires a sequence number, if we provide an invalid one the service should
            // throw an exception with the expected sequence number
            awsLogs.putLogEvents(
                    PutLogEventsRequest.builder()
                                       .logGroupName(logGroupName)
                                       .logStreamName(logStreamName)
                                       .logEvents(InputLogEvent.builder().message(LOG_MESSAGE).timestamp(LOG_MESSAGE_TIMESTAMP).build())
                                       .sequenceToken("invalid")
                                       .build());
        } catch (InvalidSequenceTokenException e) {
            assertNotNull(e.expectedSequenceToken());
        }
    }

    @Test
    public void testRetentionPolicy() {
        awsLogs.putRetentionPolicy(PutRetentionPolicyRequest.builder()
                                                            .logGroupName(logGroupName)
                                                            .retentionInDays(LOG_RETENTION_DAYS)
                                                            .build());

        // Use DescribeLogGroup to verify the updated retention policy
        LogGroup group = findLogGroupByName(awsLogs, logGroupName);
        Assert.assertNotNull(group);
        Assert.assertEquals(LOG_RETENTION_DAYS, group.retentionInDays().intValue());

        awsLogs.deleteRetentionPolicy(DeleteRetentionPolicyRequest.builder().logGroupName(logGroupName).build());

        // Again, use DescribeLogGroup to verify that the retention policy has been deleted
        group = findLogGroupByName(awsLogs, logGroupName);
        Assert.assertNotNull(group);
        Assert.assertNull(group.retentionInDays());
    }

    /**
     * Test creating a log metric filter for the specified group.
     */
    public void testCreateMetricFilter(final String groupName, final String filterName) {
        awsLogs.putMetricFilter(PutMetricFilterRequest.builder()
                                                      .logGroupName(groupName)
                                                      .filterName(filterName)
                                                      .filterPattern(LOG_METRIC_FILTER_PATTERN)
                                                      .metricTransformations(MetricTransformation.builder()
                                                                                                 .metricName(CLOUDWATCH_METRIC_NAME)
                                                                                                 .metricNamespace(CLOUDWATCH_METRIC_NAMESPACE)
                                                                                                 .metricValue("$content")
                                                                                                 .build())
                                                      .build());

        final MetricFilter mf = findMetricFilterByName(awsLogs, groupName, filterName);

        Assert.assertNotNull(
                String.format("Metric filter [%s] is not found in the [%s] log group.", filterName, groupName), mf);

        Assert.assertEquals(filterName, mf.filterName());
        Assert.assertEquals(LOG_METRIC_FILTER_PATTERN, mf.filterPattern());
        Assert.assertNotNull(mf.creationTime());
        Assert.assertNotNull(mf.metricTransformations());

        // Use DescribeLogGroups to verify that LogGroup.metricFilterCount is updated
        final LogGroup group = findLogGroupByName(awsLogs, logGroupName);
        Assert.assertEquals(1, group.metricFilterCount().intValue());
    }

}
