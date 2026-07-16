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

import static java.util.Collections.unmodifiableSet;
import static software.amazon.awssdk.http.HttpMetric.AVAILABLE_CONCURRENCY;
import static software.amazon.awssdk.http.HttpMetric.CONCURRENCY_ACQUIRE_DURATION;
import static software.amazon.awssdk.http.HttpMetric.LEASED_CONCURRENCY;
import static software.amazon.awssdk.http.HttpMetric.MAX_CONCURRENCY;
import static software.amazon.awssdk.http.HttpMetric.PENDING_CONCURRENCY_ACQUIRES;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionException;
import javax.net.ssl.SSLHandshakeException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.crt.http.HttpManagerMetrics;
import software.amazon.awssdk.crt.http.HttpStreamManager;
import software.amazon.awssdk.metrics.MetricCollector;

@SdkInternalApi
public final class CrtUtils {
    public static final int CRT_TLS_NEGOTIATION_ERROR_CODE = 1029;
    /**
     * Corresponds to CRT error: AWS_IO_SOCKET_TIMEOUT
     */
    public static final int CRT_SOCKET_TIMEOUT = 1048;

    // HTTP error codes that the native CRT classifier (CRT.awsIsTransientError) does NOT mark as transient
    // but that the SDK considers recoverable. See enum aws_http_errors in aws-c-http/include/aws/http/http.h
    // for symbolic names.
    private static final Set<Integer> ADDITIONAL_RETRYABLE_ERROR_CODES;

    static {
        Set<Integer> codes = new HashSet<>();
        // AWS_ERROR_HTTP_CHANNEL_THROUGHPUT_FAILURE (health check failure)
        codes.add(2073);
        // AWS_ERROR_HTTP_GOAWAY_RECEIVED
        codes.add(2076);
        // AWS_ERROR_HTTP_MAX_CONCURRENT_STREAMS_EXCEEDED
        codes.add(2085);
        // AWS_ERROR_HTTP_STREAM_MANAGER_CONNECTION_ACQUIRE_FAILURE
        codes.add(2087);
        // AWS_ERROR_HTTP_RESPONSE_FIRST_BYTE_TIMEOUT
        codes.add(2092);
        // AWS_ERROR_HTTP_CONNECTION_MANAGER_ACQUISITION_TIMEOUT
        codes.add(2093);
        // AWS_ERROR_HTTP_CONNECTION_MANAGER_MAX_PENDING_ACQUISITIONS_EXCEEDED
        codes.add(2094);
        ADDITIONAL_RETRYABLE_ERROR_CODES = unmodifiableSet(codes);
    }


    private CrtUtils() {
    }

    public static Throwable wrapWithIoExceptionIfRetryable(HttpException httpException) {
        Throwable toThrow = httpException;
        int errorCode = httpException.getErrorCode();

        if (CRT.awsIsTransientError(errorCode) ||
            ADDITIONAL_RETRYABLE_ERROR_CODES.contains(errorCode)) {
            toThrow = new IOException(httpException);
        }
        return toThrow;
    }

    public static Throwable wrapCrtException(Throwable throwable) {
        if (throwable instanceof CompletionException) {
            throwable = throwable.getCause();
        }
        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            int httpErrorCode = httpException.getErrorCode();

            if (httpErrorCode == CRT_TLS_NEGOTIATION_ERROR_CODE) {
                SSLHandshakeException sslHandshakeException = new SSLHandshakeException(httpException.getMessage());
                sslHandshakeException.initCause(httpException);
                return sslHandshakeException;
            }

            if (httpErrorCode == CRT_SOCKET_TIMEOUT) {
                ConnectException connectException = new ConnectException(httpException.getMessage());
                connectException.initCause(httpException);
                return connectException;
            }

            return wrapWithIoExceptionIfRetryable((HttpException) throwable);
        }

        if (throwable instanceof IllegalStateException || throwable instanceof CrtRuntimeException) {
            // CRT throws IllegalStateException if the connection is closed
            return new IOException("An exception occurred when making the request", throwable);
        }

        return throwable;
    }

    public static void reportMetrics(HttpStreamManager connManager, MetricCollector metricCollector,
                                     long acquireStartTime) {
        long acquireCompletionTime = System.nanoTime();
        Duration acquireTimeTaken = Duration.ofNanos(acquireCompletionTime - acquireStartTime);
        metricCollector.reportMetric(CONCURRENCY_ACQUIRE_DURATION, acquireTimeTaken);
        HttpManagerMetrics managerMetrics = connManager.getManagerMetrics();
        metricCollector.reportMetric(MAX_CONCURRENCY, connManager.getMaxConnections());
        metricCollector.reportMetric(AVAILABLE_CONCURRENCY, saturatedCast(managerMetrics.getAvailableConcurrency()));
        metricCollector.reportMetric(LEASED_CONCURRENCY, saturatedCast(managerMetrics.getLeasedConcurrency()));
        metricCollector.reportMetric(PENDING_CONCURRENCY_ACQUIRES, saturatedCast(managerMetrics.getPendingConcurrencyAcquires()));
    }

}
