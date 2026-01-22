/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.crt.internal;

import static software.amazon.awssdk.http.HttpMetric.AVAILABLE_CONCURRENCY;
import static software.amazon.awssdk.http.HttpMetric.CONCURRENCY_ACQUIRE_DURATION;
import static software.amazon.awssdk.http.HttpMetric.LEASED_CONCURRENCY;
import static software.amazon.awssdk.http.HttpMetric.MAX_CONCURRENCY;
import static software.amazon.awssdk.http.HttpMetric.PENDING_CONCURRENCY_ACQUIRES;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import javax.net.ssl.SSLHandshakeException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpClientConnection;
import software.amazon.awssdk.crt.http.HttpClientConnectionManager;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.crt.http.HttpManagerMetrics;
import software.amazon.awssdk.metrics.MetricCollector;

@SdkInternalApi
public final class CrtUtils {
    public static final int CRT_TLS_NEGOTIATION_ERROR_CODE = 1029;
    public static final int CRT_SOCKET_TIMEOUT = 1048;

    private CrtUtils() {
    }

    public static Throwable wrapWithIoExceptionIfRetryable(HttpException httpException) {
        Throwable toThrow = httpException;

        if (HttpClientConnection.isErrorRetryable(httpException)) {
            // IOExceptions get retried, and if the CRT says this error is retryable,
            // it's semantically an IOException anyway.
            toThrow = new IOException(httpException);
        }
        return toThrow;
    }

    public static Throwable wrapConnectionFailureException(Throwable throwable) {
        Throwable toThrow = new IOException("An exception occurred when acquiring a connection", throwable);
        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            int httpErrorCode = httpException.getErrorCode();

            if (httpErrorCode == CRT_TLS_NEGOTIATION_ERROR_CODE) {
                toThrow = new SSLHandshakeException(httpException.getMessage());
            } else if (httpErrorCode == CRT_SOCKET_TIMEOUT) {
                toThrow = new ConnectException(httpException.getMessage());
            }
        }

        return toThrow;
    }

    public static void reportMetrics(HttpClientConnectionManager connManager, MetricCollector metricCollector,
                                      long acquireStartTime) {
        long acquireCompletionTime = System.nanoTime();
        Duration acquireTimeTaken = Duration.ofNanos(acquireCompletionTime - acquireStartTime);
        metricCollector.reportMetric(CONCURRENCY_ACQUIRE_DURATION, acquireTimeTaken);
        HttpManagerMetrics managerMetrics = connManager.getManagerMetrics();
        // currently this executor only handles HTTP 1.1. Until H2 is added, the max concurrency settings are 1:1 with TCP
        // connections. When H2 is added, this code needs to be updated to handle stream multiplexing
        metricCollector.reportMetric(MAX_CONCURRENCY, connManager.getMaxConnections());
        metricCollector.reportMetric(AVAILABLE_CONCURRENCY, saturatedCast(managerMetrics.getAvailableConcurrency()));
        metricCollector.reportMetric(LEASED_CONCURRENCY, saturatedCast(managerMetrics.getLeasedConcurrency()));
        metricCollector.reportMetric(PENDING_CONCURRENCY_ACQUIRES, saturatedCast(managerMetrics.getPendingConcurrencyAcquires()));
    }

}
