/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.testutils.service.AwsTestBase.isValidSdkServiceException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.SdkGlobalTime;
import software.amazon.awssdk.core.auth.StaticCredentialsProvider;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.DeleteAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsForMetricResponse;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
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
import software.amazon.awssdk.services.cloudwatch.model.StateValue;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

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
        cloudwatch = CloudWatchClient.builder()
                                     .credentialsProvider(getCredentialsProvider())
                                     .region(Region.US_WEST_2)
                                     .build();
    }

    /**
     * Cleans up any existing alarms before and after running the test suite
     */
    @AfterClass
    public static void cleanupAlarms() {
        if (cloudwatch != null) {
            DescribeAlarmsResponse describeResult = cloudwatch.describeAlarms(DescribeAlarmsRequest.builder().build());
            Collection<String> toDelete = new LinkedList<>();
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
                                             .metricName(measureName).timestamp(Instant.now())
                                             .unit("Count").value(42.0).build();

        cloudwatch.putMetricData(PutMetricDataRequest.builder()
                                         .namespace("AWS.EC2").metricData(datum).build());

        GetMetricStatisticsResponse result =
                Waiter.run(() -> cloudwatch.getMetricStatistics(r -> r.startTime(Instant.now().minus(Duration.ofDays(7)))
                                                                      .namespace("AWS.EC2")
                                                                      .period(60 * 60)
                                                                      .dimensions(Dimension.builder().name("InstanceType")
                                                                                           .value("m1.small").build())
                                                                      .metricName(measureName)
                                                                      .statistics("Average", "Maximum", "Minimum", "Sum")
                                                                      .endTime(Instant.now())))
                      .until(r -> r.datapoints().size() == 1)
                      .orFailAfter(Duration.ofMinutes(1));

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

        Collection<MetricDatum> data = new LinkedList<>();
        for (int i = ONE_WEEK_IN_MILLISECONDS; i >= 0; i -= ONE_HOUR_IN_MILLISECONDS) {
            long time = now - i;
            MetricDatum datum = MetricDatum.builder().dimensions(
                    Dimension.builder().name("InstanceType").value("m1.small").build())
                                                 .metricName(measureName).timestamp(Instant.now())
                                                 .unit("Count").value(value).build();
            data.add(datum);
        }

        try {
            cloudwatch.putMetricData(PutMetricDataRequest.builder().namespace(
                    "AWS/EC2").metricData(data).build());
            fail("Expected an error");
        } catch (SdkServiceException e) {
            assertTrue(413 == e.statusCode());
        }
    }

    /**
     * Tests setting the state for an alarm and reading its history.
     */
    @Test
    public void describe_alarms_returns_values_set() {
        String metricName = this.getClass().getName() + System.currentTimeMillis();

        List<PutMetricAlarmRequest> rqs = createTwoNewAlarms(metricName);

        rqs.forEach(rq -> cloudwatch.setAlarmState(r -> r.alarmName(rq.alarmName()).stateValue("ALARM").stateReason("manual")));

        DescribeAlarmsForMetricResponse describeResult = describeAlarmsForMetric(rqs);

        assertThat(describeResult.metricAlarms(), hasSize(2));

        //check the state is correct
        describeResult.metricAlarms().forEach(alarm -> {
            assertThat(alarm.alarmName(), isIn(rqs.stream().map(PutMetricAlarmRequest::alarmName).collect(toList())));
            assertThat(alarm.stateValue(), equalTo(StateValue.ALARM));
            assertThat(alarm.stateReason(), equalTo("manual"));
        });

        //check the state history has been recorded
        rqs.stream().map(alarm -> cloudwatch.describeAlarmHistory(r -> r.alarmName(alarm.alarmName())
                                                                        .historyItemType(HistoryItemType.STATE_UPDATE)))
           .forEach(history -> assertThat(history.alarmHistoryItems(), hasSize(greaterThan(0))));
    }

    /**
     * Tests disabling and enabling alarm actions
     */
    @Test
    public void disable_enable_alarms_returns_success() {
        String metricName = this.getClass().getName() + System.currentTimeMillis();

        List<PutMetricAlarmRequest> rqs = createTwoNewAlarms(metricName);
        List<String> alarmNames = rqs.stream().map(PutMetricAlarmRequest::alarmName).collect(toList());

        PutMetricAlarmRequest rq1 = rqs.get(0);
        PutMetricAlarmRequest rq2 = rqs.get(1);

        /*
         * Disable
         */
        cloudwatch.disableAlarmActions(r -> r.alarmNames(alarmNames));

        DescribeAlarmsForMetricResponse describeDisabledResult = describeAlarmsForMetric(rqs);

        assertThat(describeDisabledResult.metricAlarms(), hasSize(2));

        describeDisabledResult.metricAlarms().forEach(alarm -> {
            assertThat(alarm.alarmName(), isIn(alarmNames));
            assertThat(alarm.actionsEnabled(), is(false));
        });

        /*
         * Enable
         */
        cloudwatch.enableAlarmActions(r -> r.alarmNames(alarmNames));

        DescribeAlarmsForMetricResponse describeEnabledResult = describeAlarmsForMetric(rqs);

        assertThat(describeEnabledResult.metricAlarms(), hasSize(2));
        describeEnabledResult.metricAlarms().forEach(alarm -> {
            assertThat(alarm.alarmName(), isIn(alarmNames));
            assertThat(alarm.actionsEnabled(), is(true));
        });
    }

    private DescribeAlarmsForMetricResponse describeAlarmsForMetric(List<PutMetricAlarmRequest> rqs) {
        return cloudwatch.describeAlarmsForMetric(r -> r.dimensions(rqs.get(0).dimensions())
                                                        .metricName(rqs.get(0).metricName())
                                                        .namespace(rqs.get(0).namespace()));
    }

    /**
     * Creates two alarms on the metric name given and returns the two requests
     * as an array.
     */
    private List<PutMetricAlarmRequest> createTwoNewAlarms(String metricName) {

        List<PutMetricAlarmRequest> rqs = new ArrayList<>();

        /*
         * Create & put two metric alarms
         */
        rqs.add(PutMetricAlarmRequest.builder().actionsEnabled(true)
                                            .alarmDescription("Some alarm description").alarmName(
                        "An Alarm Name" + metricName).comparisonOperator(
                        "GreaterThanThreshold").dimensions(
                        Dimension.builder().name("InstanceType").value(
                                "m1.small").build()).evaluationPeriods(1)
                                            .metricName(metricName).namespace("AWS/EC2")
                                            .period(60).statistic("Average").threshold(1.0)
                                            .unit("Count")
                .build());


        rqs.add(PutMetricAlarmRequest.builder().actionsEnabled(true)
                                            .alarmDescription("Some alarm description 2")
                                            .alarmName("An Alarm Name 2" + metricName)
                                            .comparisonOperator("GreaterThanThreshold").dimensions(
                        Dimension.builder().name("InstanceType").value(
                                "m1.small").build()).evaluationPeriods(1)
                                            .metricName(metricName).namespace("AWS/EC2")
                                            .period(60).statistic("Average").threshold(2.0)
                                            .unit("Count")
                .build());

        rqs.forEach(cloudwatch::putMetricAlarm);
        return rqs;
    }

    /**
     * Tests that an error response from CloudWatch is correctly unmarshalled
     * into an SdkServiceException object.
     */
    @Test
    public void testExceptionHandling() throws Exception {
        try {
            cloudwatch.getMetricStatistics(GetMetricStatisticsRequest.builder()
                                                   .namespace("fake-namespace").build());
            fail("Expected an SdkServiceException, but wasn't thrown");
        } catch (SdkServiceException e) {
            assertThat(e, isValidSdkServiceException());
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
                .credentialsProvider(StaticCredentialsProvider.create(getCredentials()))
                .build();
        cloudwatch.listMetrics(ListMetricsRequest.builder().build());
        assertTrue(SdkGlobalTime.getGlobalTimeOffset() < 3600);
        // subsequent changes to the global time offset won't affect existing client
        SdkGlobalTime.setGlobalTimeOffset(3600);
        cloudwatch.listMetrics(ListMetricsRequest.builder().build());
        assertTrue(SdkGlobalTime.getGlobalTimeOffset() == 3600);
    }
}
