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
package software.amazon.awssdk.http.urlconnection;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.internal.util.ResponseHandlerTestUtils.combinedSyncResponseHandler;
import static utils.HttpTestUtils.executionContext;
import static utils.HttpTestUtils.testClientConfiguration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.internal.http.response.NullErrorResponseHandler;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

public class UrlConnectionHttpClientRetryTest {
    @Test
    void execute_whenOutputStreamThrowsBareNpe_retriesRequest() {
        verifyOutputStreamFailureIsRetried(() -> {
            throw new NullPointerException("this.http is null");
        });
    }

    @Test
    void execute_whenOutputStreamThrowsWrappedNpe_retriesRequest() {
        verifyOutputStreamFailureIsRetried(() -> {
            throw new RuntimeException(new NullPointerException("this.http is null"));
        });
    }

    @Test
    void execute_whenOutputStreamThrowsIOException_retriesRequest() {
        verifyOutputStreamFailureIsRetried(() -> {
            throw new IOException("connection closed");
        });
    }

    @Test
    void execute_whenInputStreamThrowsBareNpe_retriesRequest() {
        AtomicInteger attempts = new AtomicInteger();
        SdkHttpClient transport = UrlConnectionHttpClient.create(uri -> new StubHttpURLConnection(toUrl(uri)) {
            @Override
            public InputStream getInputStream() {
                if (attempts.incrementAndGet() == 1) {
                    throw new NullPointerException("this.http is null");
                }
                return new ByteArrayInputStream(new byte[0]);
            }

            // Ensure responseHasNoContent() proceeds to getInputStream().
            @Override
            public String getHeaderField(String name) {
                return null;
            }
        });

        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .uri(URI.create("http://localhost/test"))
                                                       .method(SdkHttpMethod.GET)
                                                       .build();
        verifyFailureIsRetried(transport, request, attempts);
    }

    @Test
    void execute_whenResponseCodeCheckBeforeInputStreamThrowsIOException_retriesRequest() {
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger responseCodeCalls = new AtomicInteger();
        SdkHttpClient transport = UrlConnectionHttpClient.create(uri -> {
            attempts.incrementAndGet();
            return new StubHttpURLConnection(toUrl(uri)) {
                @Override
                public int getResponseCode() throws IOException {
                    if (responseCodeCalls.incrementAndGet() == 2) {
                        throw new IOException("connection closed");
                    }
                    return HTTP_OK;
                }
            };
        });

        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .uri(URI.create("http://localhost/test"))
                                                       .method(SdkHttpMethod.GET)
                                                       .build();
        verifyFailureIsRetried(transport, request, attempts);
    }

    private void verifyOutputStreamFailureIsRetried(IoRunnable firstAttemptFailure) {
        AtomicInteger attempts = new AtomicInteger();
        SdkHttpClient transport = UrlConnectionHttpClient.create(uri -> new StubHttpURLConnection(toUrl(uri)) {
            @Override
            public OutputStream getOutputStream() throws IOException {
                if (attempts.incrementAndGet() == 1) {
                    firstAttemptFailure.run();
                }
                return super.getOutputStream();
            }
        });

        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .uri(URI.create("http://localhost/test"))
                                                       .method(SdkHttpMethod.PUT)
                                                       .putHeader("Content-Length", "1")
                                                       .contentStreamProvider(() -> new ByteArrayInputStream(new byte[1]))
                                                       .build();
        verifyFailureIsRetried(transport, request, attempts);
    }

    private void verifyFailureIsRetried(SdkHttpClient transport,
                                         SdkHttpFullRequest request,
                                         AtomicInteger attempts) {
        RetryPolicy retryPolicy = RetryPolicy.builder()
                                             .numRetries(1)
                                             .backoffStrategy(BackoffStrategy.none())
                                             .throttlingBackoffStrategy(BackoffStrategy.none())
                                             .build();
        AmazonSyncHttpClient client = new AmazonSyncHttpClient(
            testClientConfiguration().toBuilder()
                                     .option(SdkClientOption.SYNC_HTTP_CLIENT, transport)
                                     .option(SdkClientOption.RETRY_POLICY, retryPolicy)
                                     .build());
        try {
            client.requestExecutionBuilder()
                  .request(request)
                  .originalRequest(NoopTestRequest.builder().build())
                  .executionContext(executionContext(request))
                  .execute(combinedSyncResponseHandler(null, new NullErrorResponseHandler()));
        } finally {
            client.close();
        }

        assertThat(attempts.get()).isEqualTo(2);
    }

    private static URL toUrl(URI uri) {
        try {
            return uri.toURL();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @FunctionalInterface
    private interface IoRunnable {
        void run() throws IOException;
    }

    private static class StubHttpURLConnection extends HttpURLConnection {
        private StubHttpURLConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() {
            connected = true;
        }

        @Override
        public void disconnect() {
            connected = false;
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return new ByteArrayOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return null;
        }

        @Override
        public int getResponseCode() throws IOException {
            return HTTP_OK;
        }

        @Override
        public String getResponseMessage() {
            return "OK";
        }

        @Override
        public String getHeaderField(String name) {
            return "Content-Length".equals(name) ? "0" : null;
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return Collections.emptyMap();
        }
    }
}
