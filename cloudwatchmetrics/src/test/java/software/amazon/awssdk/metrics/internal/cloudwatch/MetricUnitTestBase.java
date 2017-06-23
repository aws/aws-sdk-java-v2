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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;

/**
 * This class provides helper methods and performs the initial setup for SDK
 * Metrics unit testing.
 * 
 */
public class MetricUnitTestBase {
    protected static final int QUEUE_TIMEOUT_MILLI = 2000;

    /** Client Object Reference for Amazon CloudWatch. */
    protected static CloudWatchTestClient cloudWatchClient;

    /** Object Reference for Metrics configuration used in testing. */
    protected static CloudWatchMetricConfig config;

    /** Object reference to the metrics queue. */
    protected static BlockingQueue<MetricDatum> queue;

    /** A list that holds the test metric records to be used for the testing. */
    protected static List<String> records = new ArrayList<>();

    static {

        records.add("AmazonDynamoDBv2|HttpRequestTime||1371661280410|535.452||Milliseconds");
        records.add("AmazonDynamoDBv2|Retry Count||1371661280411|0.0||Count");
        records.add("AmazonDynamoDBv2|HttpRequestTime||1371661282748|476.031||Milliseconds");
        records.add("AmazonDynamoDBv2|Retry Count||1371661282748|0.0||Count");
        records.add("AmazonDynamoDBv2|HttpRequestTime||1371661302879|102.185||Milliseconds");
        records.add("AmazonDynamoDBv2|Retry Count||1371661302879|0.0||Count");
        records.add("AmazonDynamoDBv2|HttpRequestTime||1371661322980|93.704||Milliseconds");
        records.add("AmazonDynamoDBv2|Retry Count||1371661322980|0.0||Count");
        records.add("AmazonDynamoDBv2|HttpRequestTime||1371661343088||SampleCount=5,Sum=1000,Maximum=400,Minimum=100|Milliseconds");
        records.add("AmazonDynamoDBv2|Retry Count||1371661343088|0.0||Count");
        records.add("AmazonDynamoDBv2|HttpRequestTime|Server=alpha|1371661349001|126.841||Milliseconds");
        records.add("AmazonDynamoDBv2|Retry Count|Server=alpha|1371661349001|0.0||Count");
        records.add("AmazonDynamoDBv2||Server=alpha|1371661349001|0.0||Count");
        records.add("AmazonDynamoDBv2|HttpRequestTime||1371661280410|535.452||Milliseconds");
        records.add("AmazonDynamoDBv2|Retry Count||1371661280411|0.0||Count");
        records.add("AmazonDynamoDBv2|HttpRequestTime||1371661282748|476.031||Milliseconds");
        records.add("AmazonDynamoDBv2|Retry Count||1371661282748|0.0||Count");
        records.add("AmazonDynamoDBv2|HttpRequestTime||1371661302879|102.185||Milliseconds");
        records.add("AmazonDynamoDBv2|Retry Count||1371661302879|0.0||Count");
        records.add("AmazonDynamoDBv2|HttpRequestTime||1371661322980|93.704||Milliseconds");
        records.add("AmazonDynamoDBv2|Retry Count||1371661322980|0.0||Count");
        records.add("AmazonDynamoDBv2|HttpRequestTime||1371661343088||SampleCount=5,Sum=1000,Maximum=400,Minimum=100|Milliseconds");
        records.add("AmazonDynamoDBv2|Retry Count||1371661343088|0.0||Count");
        records.add("AmazonDynamoDBv2|HttpRequestTime|Server=alpha|1371661349001|126.841||Milliseconds");
        records.add("AmazonDynamoDBv2|Retry Count|Server=alpha|1371661349001|0.0||Count");
        records.add("AmazonDynamoDBv2|Retry Count|a=b,b=c,c=d,d=e,e=f,f=g,g=h,h=i,i=j,j=k,k=l,l=m|1371661349001|0.0||Count");
        records.add("AmazonDynamoDBv2||Server=alpha|1371661349001|0.0||Count");
        records.add("AmazonDynamoDBv2|HttpRequestTime||1371661343088||SampleCount=5,Sum=1000,Maximum=400,Average=100|Milliseconds");
        records.add("AmazonDynamoDBv2AmazonDynamoDBv2AmazonDynamoDBv2AmazonDynamoDBv2AmazonDynamoDBv2AmazonDynamoDBv2AmazonDynamoDBv2AmazonDynamoDBv2AmazonDynamoDBv2AmazonDynamoDBv2AmazonDynamoDBv2AmazonDynamoDBv2AmazonDynamoDBv2AmazonDynamoDBv2AmazonDynamoDBv2|HttpRequestTime||1371661343088||SampleCount=5,Sum=1000,Maximum=400,Average=100|Milliseconds");
    }

    /**
     * Performs the initial setup for unit testing. Creates the metrics
     * configuration to be used for testing. Also adds to the queue the metric
     * records required for testing purposes.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        cloudWatchClient = new CloudWatchTestClient();
        queue = new LinkedBlockingQueue<>(CloudWatchMetricConfig.DEFAULT_METRICS_QSIZE);
        config = new CloudWatchMetricConfig().withQueuePollTimeoutMilli(QUEUE_TIMEOUT_MILLI).withCloudWatchClient(cloudWatchClient);
        formQueueWithMetricData();
    }

    /**
     * Helper method that adds to queue with metric records.
     */
    private static void formQueueWithMetricData() throws Exception {

        Iterator<String> it = records.iterator();
        String record;

        while (it.hasNext()) {
            record = it.next();
            String fields[] = record.split("[|]");

            MetricDatum.Builder m = MetricDatum.builder();
            final String metricName = fields[0];
            final String metric = fields[1];
            m.metricName(metricName)
             .dimensions(Dimension.builder().name("metric").value(metric).build())
             .timestamp(new Date(Long.parseLong(fields[3])));
            if (fields[4] != null && !fields[4].isEmpty())
                m.value(Double.parseDouble(fields[4]));
            m.unit(fields[6]);
            queue.add(m.build());

        }
    }
}
