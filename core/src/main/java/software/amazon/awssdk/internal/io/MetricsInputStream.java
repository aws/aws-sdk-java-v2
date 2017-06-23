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

package software.amazon.awssdk.internal.io;

import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;

/**
 * Record input stream read time into {@link AwsRequestMetrics} under metric type
 * HttpSocketReadTime;
 */
public class MetricsInputStream extends DelegateInputStream {

    private AwsRequestMetrics metrics;

    public MetricsInputStream(InputStream in) {
        super(in);
    }

    public void setMetrics(AwsRequestMetrics metrics) {
        this.metrics = metrics;
    }

    /**
     * {@inheritDoc}
     * Record the read time into the metrics.
     */
    @Override
    public int read() throws IOException {
        if (metrics != null) {
            metrics.startEvent(AwsRequestMetrics.Field.HttpSocketReadTime);
        }
        try {
            return in.read();
        } finally {
            if (metrics != null) {
                metrics.endEvent(AwsRequestMetrics.Field.HttpSocketReadTime);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Record the read time into the metrics.
     */
    @Override
    public int read(byte[] b) throws IOException {
        if (metrics != null) {
            metrics.startEvent(AwsRequestMetrics.Field.HttpSocketReadTime);
        }
        try {
            return in.read(b);
        } finally {
            if (metrics != null) {
                metrics.endEvent(AwsRequestMetrics.Field.HttpSocketReadTime);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Record the read time into the metrics.
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (metrics != null) {
            metrics.startEvent(AwsRequestMetrics.Field.HttpSocketReadTime);
        }
        try {
            return in.read(b, off, len);
        } finally {
            if (metrics != null) {
                metrics.endEvent(AwsRequestMetrics.Field.HttpSocketReadTime);
            }
        }
    }
}
