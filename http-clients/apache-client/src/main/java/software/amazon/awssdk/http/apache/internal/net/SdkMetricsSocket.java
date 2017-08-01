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

package software.amazon.awssdk.http.apache.internal.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import software.amazon.awssdk.http.apache.internal.io.MetricsInputStream;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;

/**
 * A wrapper to Socket with metrics feature to record socket read time.
 */
public class SdkMetricsSocket extends DelegateSocket {

    private MetricsInputStream metricsIs;

    public SdkMetricsSocket(Socket sock) {
        super(sock);
    }

    /**
     * {@link AwsRequestMetrics} is set per request.
     */
    public void setMetrics(AwsRequestMetrics metrics) {
        if (metricsIs == null) {
            throw new IllegalStateException(
                    "The underlying input stream must be initialized!");
        }
        metricsIs.setMetrics(metrics);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        metricsIs = new MetricsInputStream(sock.getInputStream());
        return metricsIs;
    }

}
