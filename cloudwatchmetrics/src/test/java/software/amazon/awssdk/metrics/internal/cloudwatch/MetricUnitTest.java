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

import org.junit.Test;

/**
 * This class performs the unit testing for SDK Metrics feature.
 * 
 */
public class MetricUnitTest extends MetricUnitTestBase {

    /**
     * This test case provides the uploader with a queue containing the records
     * and a dummy cloud watch client. The uploader parses the data from the
     * queue and uploads the data using the dummy cloud watch client. The client
     * holds all the data in a collection which is being uploaded. If the
     * collection size is greater than zero, then the test is success.
     */
    @Test
    public void testQueueUploader() throws Exception {
        Thread t = new MetricUploaderThread(config, queue);
        t.start();
        Thread.sleep(QUEUE_TIMEOUT_MILLI + 1000);
        assertTrue(cloudWatchClient.getMetricDatums().size() > 0);
        cloudWatchClient.getMetricDatums().clear();
    }

    /**
     * This test case provides the uploader no configuration information. If an
     * exception occurs then the test case is success.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testQueueUploaderWithNullConfigInfo() throws Exception {
        new MetricUploaderThread(null, queue);
    }
}
