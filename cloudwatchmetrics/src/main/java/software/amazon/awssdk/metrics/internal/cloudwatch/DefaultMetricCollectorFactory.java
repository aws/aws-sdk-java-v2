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

import software.amazon.awssdk.AwsSystemSetting;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.metrics.AwsSdkMetrics;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

/**
 * The default AWS SDK metric collector factory.  This class is instantiated
 * via reflection if the default AWS SDK metrics is enabled via the
 * system setting {@link AwsSystemSetting#AWS_DEFAULT_METRICS}.
 */
public class DefaultMetricCollectorFactory
        implements MetricCollector.Factory {
    /**
     * Returns a instance of the Amazon CloudWatch request metric collector either by
     * starting up a new one or returning an existing one if it's already
     * started; null if any failure.
     */
    @Override
    public MetricCollector getInstance() {
        AwsCredentialsProvider provider = AwsSdkMetrics.getCredentialProvider();
        Region region = Region.of(AwsSdkMetrics.getRegionName());
        Integer qSize = AwsSdkMetrics.getMetricQueueSize();
        Long timeoutMilli = AwsSdkMetrics.getQueuePollTimeoutMilli();
        CloudWatchMetricConfig config = new CloudWatchMetricConfig();

        if (qSize != null) {
            config.setMetricQueueSize(qSize);
        }
        if (timeoutMilli != null) {
            config.setQueuePollTimeoutMilli(timeoutMilli);
        }
        config.setCloudWatchClient(CloudWatchClient.builder()
                                                   .credentialsProvider(provider)
                                                   .region(region)
                                                   .build());
        MetricCollectorSupport.startSingleton(config);
        return MetricCollectorSupport.getInstance();
    }
}
