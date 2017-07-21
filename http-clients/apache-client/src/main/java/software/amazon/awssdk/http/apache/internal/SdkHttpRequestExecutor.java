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

package software.amazon.awssdk.http.apache.internal;

import java.io.IOException;
import java.net.Socket;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import software.amazon.awssdk.http.apache.internal.net.SdkMetricsSocket;
import software.amazon.awssdk.http.apache.internal.net.SdkSslMetricsSocket;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;

/**
 * Used to capture the http send-request and receive-response latency metrics
 * of the http client library, with no retries involved.
 */
public class SdkHttpRequestExecutor extends HttpRequestExecutor {
    @Override
    protected HttpResponse doSendRequest(
            final HttpRequest request,
            final HttpClientConnection conn,
            final HttpContext context)
            throws IOException, HttpException {
        AwsRequestMetrics awsRequestMetrics = (AwsRequestMetrics) context.getAttribute(AwsRequestMetrics.class.getSimpleName());

        if (awsRequestMetrics == null) {
            return super.doSendRequest(request, conn, context);
        }
        if (conn instanceof ManagedHttpClientConnection) {
            ManagedHttpClientConnection managedConn = (ManagedHttpClientConnection) conn;
            Socket sock = managedConn.getSocket();
            if (sock instanceof SdkMetricsSocket) {
                SdkMetricsSocket sdkMetricsSocket = (SdkMetricsSocket) sock;
                sdkMetricsSocket.setMetrics(awsRequestMetrics);
            } else if (sock instanceof SdkSslMetricsSocket) {
                SdkSslMetricsSocket sdkSslMetricsSocket = (SdkSslMetricsSocket) sock;
                sdkSslMetricsSocket.setMetrics(awsRequestMetrics);
            }
        }
        awsRequestMetrics.startEvent(AwsRequestMetrics.Field.HttpClientSendRequestTime);
        try {
            return super.doSendRequest(request, conn, context);
        } finally {
            awsRequestMetrics.endEvent(AwsRequestMetrics.Field.HttpClientSendRequestTime);
        }
    }

    @Override
    protected HttpResponse doReceiveResponse(
            final HttpRequest request,
            final HttpClientConnection conn,
            final HttpContext context)
            throws HttpException, IOException {
        AwsRequestMetrics awsRequestMetrics = (AwsRequestMetrics) context
                .getAttribute(AwsRequestMetrics.class.getSimpleName());
        if (awsRequestMetrics == null) {
            return super.doReceiveResponse(request, conn, context);
        }
        awsRequestMetrics.startEvent(AwsRequestMetrics.Field.HttpClientReceiveResponseTime);
        try {
            return super.doReceiveResponse(request, conn, context);
        } finally {
            awsRequestMetrics.endEvent(AwsRequestMetrics.Field.HttpClientReceiveResponseTime);
        }
    }
}
