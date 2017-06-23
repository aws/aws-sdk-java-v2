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

package software.amazon.awssdk.services.cloudwatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.SdkGlobalTime;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.DeleteAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmHistoryRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmHistoryResponse;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsForMetricRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsForMetricResponse;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.DisableAlarmActionsRequest;
import software.amazon.awssdk.services.cloudwatch.model.EnableAlarmActionsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.HistoryItemType;
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsRequest;
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricAlarmRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.SetAlarmStateRequest;
import software.amazon.awssdk.test.AwsIntegrationTestBase;
import software.amazon.awssdk.test.util.SdkAsserts;

/**
 * Integration tests for the AWS CloudWatch service.
 */
public class CloudWatchIntegrationTest extends AwsIntegrationTestBase {

    private static final int ONE_WEEK_IN_MILLISECONDS = 1000 * 60 * 60 * 24 * 7;
    private static final int ONE_HOUR_IN_MILLISECONDS = 1000 * 60 * 60;
    /** The CloudWatch client for all tests to use. */
    private static CloudWatchClient cloudwatch;

    /**
     * Loads the AWS account info for the integration tests and creates a
     * CloudWatch client for tests to use.
     */
    @BeforeClass
    public static void setUp() throws IOException {
        cloudwatch = CloudWatchClient.builder().credentialsProvider(new StaticCredentialsProvider(getCredentials())).build();
    }

    /**
     * Cleans up any existing alarms before and after running the test suite
     */
    @AfterClass
    public static void cleanupAlarms() {
        if (cloudwatch != null) {
            DescribeAlarmsResponse describeResult = cloudwatch.describeAlarms(DescribeAlarmsRequest.builder().build());
            Collection<String> toDelete = new LinkedList<String>();
            for (MetricAlarm alarm : describeResult.metricAlarms()) {
                if (alarm.metricName().startsWith(CloudWatchIntegrationTest.class.getName())) {
                    toDelete.add(alarm.alarmName());
                }
            }
            if (!toDelete.isEmpty()) {
                DeleteAlarmsRequest delete = DeleteAlarmsRequest.builder().alarmNames(toDelete).build();
                cloudwatch.deleteAlarms(delete);
            }
        }
    }


    /**
     * Tests putting metrics and then getting them back.
     */

    @Test
    public void put_get_metricdata_list_metric_returns_success() throws
                                                                 InterruptedException {
        String measureName = this.getClass().getName() + System.currentTimeMillis();

        MetricDatum datum = MetricDatum.builder().dimensions(
                Dimension.builder().name("InstanceType").value("m1.small").build())
                                             .metricName(measureName).timestamp(new Date())
                                             .unit("Count").value(42.0).build();

        cloudwatch.putMetricData(PutMetricDataRequest.builder()
                                         .namespace("AWS.EC2").metricData(datum).build());

        // TODO: get an ETA on the arrival of this data
        Thread.sleep(60 * 1000);

        GetMetricStatisticsRequest getRequest = GetMetricStatisticsRequest.builder()
                .startTime(
                        new Date(new Date().getTime()
                                 - ONE_WEEK_IN_MILLISECONDS))
                .namespace("AWS.EC2")
                .period(60 * 60)
                .dimensions(Dimension.builder().name("InstanceType").value("m1.small").build())
                .metricName(measureName)
                .statistics("Average", "Maximum", "Minimum", "Sum")
                .endTime(new Date())
                .build();
        GetMetricStatisticsResponse result = cloudwatch
                .getMetricStatistics(getRequest);

        assertNotNull(result.label());
        assertEquals(measureName, result.label());

        assertEquals(1, result.datapoints().size());
        for (Datapoint datapoint : result.datapoints()) {
            assertEquals(datum.value(), datapoint.average());
            assertEquals(datum.value(), datapoint.maximum());
            assertEquals(datum.value(), datapoint.minimum());
            assertEquals(datum.value(), datapoint.sum());
            assertNotNull(datapoint.timestamp());
            assertEquals(datum.unit(), datapoint.unit());
        }

        ListMetricsResponse listResult = cloudwatch.listMetrics(ListMetricsRequest.builder().build());

        boolean seenDimensions = false;
        assertTrue(listResult.metrics().size() > 0);
        for (Metric metric : listResult.metrics()) {
            assertNotNull(metric.metricName());
            assertNotNull(metric.namespace());

            for (Dimension dimension : metric.dimensions()) {
                seenDimensions = true;
                assertNotNull(dimension.name());
                assertNotNull(dimension.value());
            }
        }
        assertTrue(seenDimensions);
    }


    /**
     * Tests handling a "request too large" error. This breaks our parser right
     * now and is therefore disabled.
     */

    @Test
    public void put_metric_large_data_throws_request_entity_large_exception()
            throws Exception {
        String measureName = this.getClass().getName() + System.currentTimeMillis();
        long now = System.currentTimeMillis();
        double value = 42.0;

        Collection<MetricDatum> data = new LinkedList<MetricDatum>();
        for (int i = ONE_WEEK_IN_MILLISECONDS; i >= 0; i -= ONE_HOUR_IN_MILLISECONDS) {
            long time = now - i;
            MetricDatum datum = MetricDatum.builder().dimensions(
                    Dimension.builder().name("InstanceType").value("m1.small").build())
                                                 .metricName(measureName).timestamp(new Date(time))
                                                 .unit("Count").value(value).build();
            data.add(datum);
        }

        try {
            cloudwatch.putMetricData(PutMetricDataRequest.builder().namespace(
                    "AWS/EC2").metricData(data).build());
            fail("Expected an error");
        } catch (AmazonServiceException e) {
            assertTrue(413 == e.getStatusCode());
        }
    }

    /**
     * Tests setting the state for an alarm and reading its history.
     */
    @Test
    public void describe_alarms_returns_values_set() {
        String metricName = this.getClass().getName()
                            + System.currentTimeMillis();

        PutMetricAlarmRequest[] rqs = createTwoNewAlarms(metricName);

        PutMetricAlarmRequest rq1 = rqs[0];
        PutMetricAlarmRequest rq2 = rqs[1];

        /*
         * Set the state
         */
        SetAlarmStateRequest setAlarmStateRequest = SetAlarmStateRequest.builder()
                .alarmName(rq1.alarmName()).stateValue("ALARM")
                .stateReason("manual").build();
        cloudwatch.setAlarmState(setAlarmStateRequest);
        setAlarmStateRequest = SetAlarmStateRequest.builder().alarmName(
                rq2.alarmName()).stateValue("ALARM").stateReason(
                "manual").build();
        cloudwatch.setAlarmState(setAlarmStateRequest);

        DescribeAlarmsForMetricResponse describeResult = cloudwatch
                .describeAlarmsForMetric(DescribeAlarmsForMetricRequest.builder()
                                                 .dimensions(rq1.dimensions()).metricName(
                                metricName).namespace(rq1.namespace()).build());
        assertEquals(2, describeResult.metricAlarms().size());
        for (MetricAlarm alarm : describeResult.metricAlarms()) {
            assertTrue(rq1.alarmName().equals(alarm.alarmName())
                       || rq2.alarmName().equals(alarm.alarmName()));
            assertEquals(setAlarmStateRequest.stateValue(), alarm
                    .stateValue());
            assertEquals(setAlarmStateRequest.stateReason(), alarm
                    .stateReason());
        }

        /*
         * Get the history
         */
        DescribeAlarmHistoryRequest alarmHistoryRequest = DescribeAlarmHistoryRequest.builder()
                .alarmName(rq1.alarmName()).historyItemType(HistoryItemType.StateUpdate)
                .build();
        DescribeAlarmHistoryResponse historyResult = cloudwatch
                .describeAlarmHistory(alarmHistoryRequest);
        assertEquals(1, historyResult.alarmHistoryItems().size());
    }

    /**
     * Tests disabling and enabling alarm actions
     */
    @Test
    public void disable_enable_alarms_returns_success() {
        String metricName = this.getClass().getName()
                            + System.currentTimeMillis();

        PutMetricAlarmRequest[] rqs = createTwoNewAlarms(metricName);

        PutMetricAlarmRequest rq1 = rqs[0];
        PutMetricAlarmRequest rq2 = rqs[1];

        /*
         * Disable
         */
        DisableAlarmActionsRequest disable = DisableAlarmActionsRequest.builder()
                .alarmNames(rq1.alarmName(), rq2.alarmName()).build();
        cloudwatch.disableAlarmActions(disable);

        DescribeAlarmsForMetricResponse describeResult = cloudwatch
                .describeAlarmsForMetric(DescribeAlarmsForMetricRequest.builder()
                                                 .dimensions(rq1.dimensions()).metricName(
                                metricName).namespace(rq1.namespace()).build());
        assertEquals(2, describeResult.metricAlarms().size());
        for (MetricAlarm alarm : describeResult.metricAlarms()) {
            assertTrue(rq1.alarmName().equals(alarm.alarmName())
                       || rq2.alarmName().equals(alarm.alarmName()));
            assertFalse(alarm.actionsEnabled());
        }

        /*
         * Enable
         */
        EnableAlarmActionsRequest enable = EnableAlarmActionsRequest.builder()
                .alarmNames(rq1.alarmName(), rq2.alarmName()).build();
        cloudwatch.enableAlarmActions(enable);

        describeResult = cloudwatch
                .describeAlarmsForMetric(DescribeAlarmsForMetricRequest.builder()
                                                 .dimensions(rq1.dimensions()).metricName(
                                metricName).namespace(rq1.namespace()).build());
        assertEquals(2, describeResult.metricAlarms().size());
        for (MetricAlarm alarm : describeResult.metricAlarms()) {
            assertTrue(rq1.alarmName().equals(alarm.alarmName())
                       || rq2.alarmName().equals(alarm.alarmName()));
            assertTrue(alarm.actionsEnabled());
        }
    }

    /**
     * Creates two alarms on the metric name given and returns the two requests
     * as an array.
     */
    private PutMetricAlarmRequest[] createTwoNewAlarms(String metricName) {
        PutMetricAlarmRequest[] rqs = new PutMetricAlarmRequest[2];

        /*
         * Put two metric alarms
         */
        rqs[0] = PutMetricAlarmRequest.builder().actionsEnabled(true)
                                            .alarmDescription("Some alarm description").alarmName(
                        "An Alarm Name" + metricName).comparisonOperator(
                        "GreaterThanThreshold").dimensions(
                        Dimension.builder().name("InstanceType").value(
                                "m1.small").build()).evaluationPeriods(1)
                                            .metricName(metricName).namespace("AWS/EC2")
                                            .period(60).statistic("Average").threshold(1.0)
                                            .unit("Count")
                .build();

        cloudwatch.putMetricAlarm(rqs[0]);

        rqs[1] = PutMetricAlarmRequest.builder().actionsEnabled(true)
                                            .alarmDescription("Some alarm description 2")
                                            .alarmName("An Alarm Name 2" + metricName)
                                            .comparisonOperator("GreaterThanThreshold").dimensions(
                        Dimension.builder().name("InstanceType").value(
                                "m1.small").build()).evaluationPeriods(1)
                                            .metricName(metricName).namespace("AWS/EC2")
                                            .period(60).statistic("Average").threshold(2.0)
                                            .unit("Count")
                .build();
        cloudwatch.putMetricAlarm(rqs[1]);
        return rqs;
    }

    /**
     * Tests that an error response from CloudWatch is correctly unmarshalled
     * into an AmazonServiceException object.
     */
    @Test
    public void testExceptionHandling() throws Exception {
        try {
            cloudwatch.getMetricStatistics(GetMetricStatisticsRequest.builder()
                                                   .namespace("fake-namespace").build());
            fail("Expected an AmazonServiceException, but wasn't thrown");
        } catch (AmazonServiceException e) {
            SdkAsserts.assertValidException(e);
        }
    }

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkew() {
        SdkGlobalTime.setGlobalTimeOffset(3600);

        CloudWatchClient cloudwatch = CloudWatchClient.builder()
                .credentialsProvider(new StaticCredentialsProvider(getCredentials()))
                .build();
        cloudwatch.listMetrics(ListMetricsRequest.builder().build());
        assertTrue(SdkGlobalTime.getGlobalTimeOffset() < 3600);
        // subsequent changes to the global time offset won't affect existing client
        SdkGlobalTime.setGlobalTimeOffset(3600);
        cloudwatch.listMetrics(ListMetricsRequest.builder().build());
        assertTrue(SdkGlobalTime.getGlobalTimeOffset() == 3600);
    }
}
