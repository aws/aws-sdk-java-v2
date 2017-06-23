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
import javax.net.ssl.SSLSocket;
import software.amazon.awssdk.http.apache.internal.io.MetricsInputStream;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;

/**
 * A wrapper to SSLSocket with metric feature to record socket read time.
 */
public class SdkSslMetricsSocket extends DelegateSslSocket {

    private MetricsInputStream metricsIs;

    public SdkSslMetricsSocket(SSLSocket sock) {
        super(sock);
    }

    public void setMetrics(AwsRequestMetrics metrics) throws IOException {
        // make sure metricsIs is initialized.
        getInputStream();
        metricsIs.setMetrics(metrics);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (metricsIs == null) {
            metricsIs = new MetricsInputStream(sock.getInputStream());
        }
        return metricsIs;
    }

    MetricsInputStream getMetricsInputStream() {
        return metricsIs;
    }
}
