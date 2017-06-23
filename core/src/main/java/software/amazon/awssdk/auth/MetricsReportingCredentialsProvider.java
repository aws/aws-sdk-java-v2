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

package software.amazon.awssdk.auth;

import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.utils.Validate;

/**
 * Decorates a {@link AwsCredentialsProvider} to publish credential loading time metrics.
 *
 * @see AwsRequestMetrics.Field#CredentialsRequestTime
 */
public class MetricsReportingCredentialsProvider implements AwsCredentialsProvider {
    private final AwsCredentialsProvider delegate;
    private final AwsRequestMetrics awsRequestMetrics;

    public MetricsReportingCredentialsProvider(AwsCredentialsProvider delegate, AwsRequestMetrics awsRequestMetrics) {
        this.delegate = Validate.notNull(delegate, "Delegate must not be null.");
        this.awsRequestMetrics = Validate.notNull(awsRequestMetrics, "Metrics must not be null.");
    }

    @Override
    public AwsCredentials getCredentials() {
        awsRequestMetrics.startEvent(AwsRequestMetrics.Field.CredentialsRequestTime);
        try {
            return delegate.getCredentials();
        } finally {
            awsRequestMetrics.endEvent(AwsRequestMetrics.Field.CredentialsRequestTime);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + delegate + ")";
    }
}
