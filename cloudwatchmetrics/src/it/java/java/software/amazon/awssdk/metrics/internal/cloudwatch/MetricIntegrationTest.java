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
package software.amazon.awssdk.metrics.internal.cloudwatch;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.metrics.AwsSdkMetrics;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.metrics.SimpleMetricType;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.util.AWSRequestMetrics;

/**
 * Performs the integration testing for SDK Metrics feature.
 * 
 */
public class MetricIntegrationTest extends MetricIntegrationTestBase {
    private static final boolean LAST_AN_HOUR = false;

    /**
     * Ensures that any created test resources are correctly released.
     */
    @AfterClass
    public static void tearDown() {
        try {
            dynamo.deleteTable(new DeleteTableRequest(tableName));
            Assert.assertTrue(metricCollection.stop());
        } catch (Exception e) {
        }
    }

    /**
     * Tests an normal flow of collecting the metrics and uploading to Amazon
     * CloudWatch. A DynamoDB put item request is initiated as a test case. The
     * thread then connects to Amazon CloudWatch to check if the metric data
     * collected is available.
     * @throws InterruptedException 
     */
    @Test
    public void testSDKMetricUploadToAmazonCloudWatch() throws InterruptedException {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put(dynamoDBFieldName,
                new AttributeValue(String.valueOf((int) (Math.random() * 100))));
        Random rand = new Random();
        long start = System.nanoTime();
        while (true) {
            dynamo.putItem(
                new PutItemRequest()
                    .withTableName(tableName)
                    .withItem(item)
            );
            long end = System.nanoTime();
            long elapsed = end - start;
            if (TimeUnit.NANOSECONDS.toMinutes(elapsed) >= 60 || !LAST_AN_HOUR)
                break;
            int sleepTime = rand.nextInt(10000);
            if (sleepTime < 0)
                sleepTime = 0-sleepTime;
            System.out.println("Sleeping for " + sleepTime + " ms");
            Thread.sleep(sleepTime);
        }
        MetricCollector mc = AwsSdkMetrics.getMetricCollector();
        if (mc instanceof MetricCollectorSupport) {
            MetricCollectorSupport col = (MetricCollectorSupport)mc;
            CloudWatchMetricConfig config = col.getConfig();
            long waitMilli = config.getQueuePollTimeoutMilli() + 1000;
            System.out.println("Wait for " + waitMilli + " msec for the statistics to be submitted to cloudwatch");
            Thread.sleep(waitMilli);
        }
        List<Datapoint> datapoints = waitForDatapointsToBecomeAvailable();
        assertTrue(datapoints.size() > 0);
    }

    @Test
    public void s3TestMetric() throws InterruptedException {
        // Add some bogus S3 metric type to be properly handled ie ignored
        AwsSdkMetrics.add(new SimpleMetricType() {
            @Override
            public String name() {
                return "S3TestMetric";
            }
        });
        RequestMetricCollector mc = AwsSdkMetrics.getRequestMetricCollector();
        // Fabricate a "S3TestMetric"
        DefaultRequest<?> mockRequest = new DefaultRequest<Object>("TestServiceName");
        AWSRequestMetrics mockRequestMetrics = new AWSRequestMetrics();
        mockRequestMetrics.setCounter("S3TestMetric", 999);
        mockRequest.setAWSRequestMetrics(mockRequestMetrics);
        // Pretend there is a metric collection
        mc.collectMetrics(mockRequest, null);
    }
}
