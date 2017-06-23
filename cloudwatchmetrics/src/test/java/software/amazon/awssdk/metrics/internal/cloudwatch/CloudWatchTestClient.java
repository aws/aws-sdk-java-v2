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
import java.util.List;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.waiters.CloudWatchClientWaiters;

/**
 * This class is used for testing purpose. A dummy client that collects the
 * metric datum in a list instead of sending it to Amazon CloudWatch.
 */
public class CloudWatchTestClient implements CloudWatchClient {

    /**
     * A list to hold the metric data that is sent to Amazon CloudWatch during testing.
     */
    private List<MetricDatum> metricDatums = new ArrayList<MetricDatum>();

    public PutMetricDataResponse putMetricData(PutMetricDataRequest putMetricDataRequest)
            throws AmazonServiceException, AmazonClientException {

        metricDatums.addAll(putMetricDataRequest.metricData());
        return PutMetricDataResponse.builder().build();
    }

    @Override
    public CloudWatchClientWaiters waiters() {
        return null;
    }


    public List<MetricDatum> getMetricDatums() {
        return metricDatums;
    }

    @Override
    public void close() throws Exception {
        close();
    }
}
