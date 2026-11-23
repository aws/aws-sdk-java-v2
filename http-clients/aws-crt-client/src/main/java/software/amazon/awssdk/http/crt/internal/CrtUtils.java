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
    /**
     * Corresponds to CRT error: AWS_ERROR_HTTP_CONNECTION_MANAGER_MAX_PENDING_ACQUISITIONS_EXCEEDED
     */
    public static final int CRT_CONNECTION_MANAGER_MAX_PENDING_ACQUISITIONS_EXCEEDED = 2094;
    /**
     * Corresponds to CRT error: AWS_ERROR_HTTP_STREAM_MANAGER_MAX_PENDING_ACQUISITIONS_EXCEEDED
     */
    public static final int CRT_STREAM_MANAGER_MAX_PENDING_ACQUISITIONS_EXCEEDED = 2096;
    /**
     * Corresponds to CRT error: AWS_ERROR_HTTP_STREAM_MANAGER_ACQUISITION_TIMEOUT
     */
    public static final int CRT_STREAM_MANAGER_ACQUISITION_TIMEOUT = 2097;

    /**
     * Message for a request rejected because the endpoint's pending acquisitions reached
     * {@code maxPendingConnectionAcquires}. The first sentence matches
     * {@code NettyUtils#getMessageForTooManyAcquireOperationsError} in netty-nio-client; the rest is adapted to the CRT
     * builder options.
     */
    private static final String MAX_PENDING_ACQUISITIONS_EXCEEDED_MESSAGE =
        "Maximum pending connection acquisitions exceeded. The request rate is too high for the client to keep up.\n"

        + "Consider taking any of the following actions to mitigate the issue: increase maxConcurrency, "
        + "increase maxPendingConnectionAcquires, or slow the request rate.\n"

        + "Increasing maxConcurrency can increase client throughput (unless the network interface is already fully "
        + "utilized), but can eventually start to hit operating system limitations on the number of file descriptors "
        + "used by the process. If you are already fully utilizing your network interface or cannot further increase "
        + "maxConcurrency, increasing maxPendingConnectionAcquires allows extra requests to be buffered by the client, "
        + "but can cause additional request latency and higher memory usage.\n"

        + "If the above mechanisms are not able to fix the issue, try smoothing out your requests so that large "
        + "traffic bursts cannot overload the client, being more efficient with the number of times you need to call "
        + "AWS, or by increasing the number of hosts sending requests.";

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

            if (httpErrorCode == CRT_CONNECTION_MANAGER_MAX_PENDING_ACQUISITIONS_EXCEEDED) {
                return maxPendingAcquisitionsExceeded(httpException);
            }

            return wrapWithIoExceptionIfRetryable((HttpException) throwable);
        }

        if (throwable instanceof CrtRuntimeException) {
            int crtErrorCode = ((CrtRuntimeException) throwable).errorCode;

            // The HTTP/2 stream manager reports its over-limit rejection as a CrtRuntimeException. Handle it like the HTTP/1.1
            // rejection, instead of wrapping it in the retryable IOException below.
            if (crtErrorCode == CRT_STREAM_MANAGER_MAX_PENDING_ACQUISITIONS_EXCEEDED) {
                return maxPendingAcquisitionsExceeded(throwable);
            }

            // Surface the HTTP/2 stream acquisition timeout like the HTTP/1.1 acquisition timeout, with the CRT text in the
            // IOException's message.
            if (crtErrorCode == CRT_STREAM_MANAGER_ACQUISITION_TIMEOUT) {
                return new IOException(throwable);
            }
        }

        if (throwable instanceof IllegalStateException || throwable instanceof CrtRuntimeException) {
            // CRT throws IllegalStateException if the connection is closed
            return new IOException("An exception occurred when making the request", throwable);
        }

        return throwable;
    }

    /**
     * Returns true if the given exception is the rejection {@link #wrapCrtException} creates when a request is over the
     * pending acquisition limit.
     */
    public static boolean isMaxPendingAcquisitionsExceeded(Throwable throwable) {
        if (!(throwable instanceof IllegalStateException)) {
            return false;
        }
        Throwable cause = throwable.getCause();
        return (cause instanceof HttpException
                && ((HttpException) cause).getErrorCode() == CRT_CONNECTION_MANAGER_MAX_PENDING_ACQUISITIONS_EXCEEDED)
               || (cause instanceof CrtRuntimeException
                   && ((CrtRuntimeException) cause).errorCode == CRT_STREAM_MANAGER_MAX_PENDING_ACQUISITIONS_EXCEEDED);
    }

    /**
     * Creates the exception for a request rejected because the endpoint's pending acquisitions reached
     * {@code maxPendingConnectionAcquires}. It is unchecked and not an {@link IOException}, so the SDK does not retry it,
     * matching the Netty client. It is the same type Netty's connection pool raises for this condition.
     *
     * @param crtException the CRT exception that reported the rejection
     * @return the exception to surface, with the CRT exception as its cause
     */
    private static IllegalStateException maxPendingAcquisitionsExceeded(Throwable crtException) {
        return new IllegalStateException(MAX_PENDING_ACQUISITIONS_EXCEEDED_MESSAGE, crtException);
    }

    public static void reportMetrics(HttpStreamManager connManager, MetricCollector metricCollector,
                                     long acquireStartTime) {
        long acquireCompletionTime = System.nanoTime();
        Duration acquireTimeTaken = Duration.ofNanos(acquireCompletionTime - acquireStartTime);
        metricCollector.reportMetric(CONCURRENCY_ACQUIRE_DURATION, acquireTimeTaken);
        metricCollector.reportMetric(MAX_CONCURRENCY, connManager.getMaxConnections());
        HttpManagerMetrics managerMetrics;
        try {
            managerMetrics = connManager.getManagerMetrics();
        } catch (IllegalStateException e) {
            // The client was closed while this request was in flight, so pool-state metrics are unavailable.
            return;
        }
        metricCollector.reportMetric(AVAILABLE_CONCURRENCY, saturatedCast(managerMetrics.getAvailableConcurrency()));
        metricCollector.reportMetric(LEASED_CONCURRENCY, saturatedCast(managerMetrics.getLeasedConcurrency()));
        metricCollector.reportMetric(PENDING_CONCURRENCY_ACQUIRES, saturatedCast(managerMetrics.getPendingConcurrencyAcquires()));
    }

}
