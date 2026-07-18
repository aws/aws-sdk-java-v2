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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ConnectException;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.crt.http.HttpException;

public class CrtUtilsTest {

    /**
     * Locks in retryability of HTTP error codes that are NOT classified as transient by the native
     * {@code CRT.awsIsTransientError} but should be wrapped in {@link IOException} so the SDK retry
     * layer treats them as recoverable. Codes correspond to entries in {@code enum aws_http_errors}
     * in aws-c-http.
     */
    @ParameterizedTest
    @ValueSource(ints = {
        2073, // AWS_ERROR_HTTP_CHANNEL_THROUGHPUT_FAILURE (health check failure)
        2076, // AWS_ERROR_HTTP_GOAWAY_RECEIVED
        2085, // AWS_ERROR_HTTP_MAX_CONCURRENT_STREAMS_EXCEEDED
        2087, // AWS_ERROR_HTTP_STREAM_MANAGER_CONNECTION_ACQUIRE_FAILURE
        2092, // AWS_ERROR_HTTP_RESPONSE_FIRST_BYTE_TIMEOUT
        2093, // AWS_ERROR_HTTP_CONNECTION_MANAGER_ACQUISITION_TIMEOUT
        2094  // AWS_ERROR_HTTP_CONNECTION_MANAGER_MAX_PENDING_ACQUISITIONS_EXCEEDED
    })
    public void wrapWithIoExceptionIfRetryable_retryableCode_wrapsInIOException(int errorCode) {
        HttpException httpException = new HttpException(errorCode);

        Throwable result = CrtUtils.wrapWithIoExceptionIfRetryable(httpException);

        assertThat(result).isInstanceOf(IOException.class).hasCause(httpException);
    }

    /**
     * Locks in non-retryability for codes that were deliberately considered and rejected during the
     * allowlist analysis (PR #6812 retry-classifier regression follow-up). Each code below has a
     * specific reason it must NOT auto-retry; without this guard a future contributor could re-add one
     * without revisiting the rationale.
     */
    @ParameterizedTest
    @ValueSource(ints = {
        2074, // AWS_ERROR_HTTP_PROTOCOL_ERROR - on-wire parse failure; deterministic for a misbehaving peer
        2077, // AWS_ERROR_HTTP_RST_STREAM_RECEIVED - H2 stream reset; existing H2ErrorTest pins non-retry
        2078, // AWS_ERROR_HTTP_RST_STREAM_SENT - symmetric to 2077
        2079, // AWS_ERROR_HTTP_STREAM_NOT_ACTIVATED - API misuse on manual write
        2080, // AWS_ERROR_HTTP_STREAM_HAS_COMPLETED - API misuse or secondary signal during teardown
        2095  // AWS_ERROR_HTTP_STREAM_CANCELLED - explicit cancel by SDK abort path; not server-induced
    })
    public void wrapWithIoExceptionIfRetryable_nonRetryableCode_returnsHttpExceptionUnchanged(int errorCode) {
        HttpException httpException = new HttpException(errorCode);

        Throwable result = CrtUtils.wrapWithIoExceptionIfRetryable(httpException);

        assertThat(result).isSameAs(httpException);
    }

    /**
     * TLS negotiation failures must chain the original {@link HttpException} (with its CRT error code)
     * as cause, so callers can differentiate transient from persistent failures.
     */
    @Test
    public void wrapCrtException_tlsNegotiationError_wrapsInSslHandshakeExceptionWithCause() {
        HttpException httpException = new HttpException(CrtUtils.CRT_TLS_NEGOTIATION_ERROR_CODE);

        Throwable result = CrtUtils.wrapCrtException(httpException);

        assertThat(result).isInstanceOf(SSLHandshakeException.class)
                          .hasMessage(httpException.getMessage())
                          .hasCause(httpException);
        assertThat(((HttpException) result.getCause()).getErrorCode())
            .isEqualTo(CrtUtils.CRT_TLS_NEGOTIATION_ERROR_CODE);
    }

    /**
     * Socket timeouts must chain the original {@link HttpException} as cause.
     */
    @Test
    public void wrapCrtException_socketTimeout_wrapsInConnectExceptionWithCause() {
        HttpException httpException = new HttpException(CrtUtils.CRT_SOCKET_TIMEOUT);

        Throwable result = CrtUtils.wrapCrtException(httpException);

        assertThat(result).isInstanceOf(ConnectException.class)
                          .hasMessage(httpException.getMessage())
                          .hasCause(httpException);
        assertThat(((HttpException) result.getCause()).getErrorCode())
            .isEqualTo(CrtUtils.CRT_SOCKET_TIMEOUT);
    }
}
