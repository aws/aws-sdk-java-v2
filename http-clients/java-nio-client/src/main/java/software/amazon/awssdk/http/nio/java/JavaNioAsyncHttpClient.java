/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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


package software.amazon.awssdk.http.nio.java;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLParameters;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.java.internal.JavaHttpRequestExecutor;
import software.amazon.awssdk.utils.AttributeMap;


/**
 * An implementation of {@link SdkAsyncHttpClient} that uses a Java HTTP Client to communicate with the service.
 *
 * <p>This can be created via {@link @builder()}</p>
 */
@SdkPublicApi
public final class JavaNioAsyncHttpClient implements SdkAsyncHttpClient {

    private static final String CLIENT_NAME = "JavaNio";
    private final HttpClient javaHttpClient;

    private JavaNioAsyncHttpClient(DefaultBuilder builder, AttributeMap serviceDefaultsMap) {
        /* JavaHttpClientConfiguration configuration = new JavaHttpClientConfiguration(serviceDefaultsMap); */
        javaHttpClient = createHttpClient();
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
        return new JavaHttpRequestExecutor(javaHttpClient, request).execute();
    }


    /**
     * Create the internal HttpClient inside JavaNioAsyncHttpClient
     *
     * @return  HttpClient object
     */
    private HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }


    public static Builder builder() {
        return new DefaultBuilder();
    }


    public void close() {
    }

    @Override
    public String clientName() {
        return CLIENT_NAME;
    }


    /**
     * Builder that allows configuration of the Java NIO HTTP implementation. Use {@link #builder()} to configure
     * and construct a Java Http Client.
     */
    public interface Builder extends SdkAsyncHttpClient.Builder<JavaNioAsyncHttpClient.Builder> {

        /**
         * Maximum number of allowed concurrent requests.
         *
         * <p>
         * If the maximum number of concurrent requests is exceeded they may queued in the HTTP Client and can
         * cause latencies. If the client is overloaded enough such that the pending requests queue fills up,
         * then subsequent requests may be rejected or time out.
         * </p>
         *
         * @param maxConcurrency New value for max concurrency.
         * @return This builder for method chaining.
         */
        Builder maxConcurrency(Integer maxConcurrency);


        /**
         * The amount of time to wait for a connection before an exeception is thrown.
         *
         * @param connectionTimeout timeout duration.
         * @return This builder for method chaining.
         */
        Builder connectionTimeout(Duration connectionTimeout);


        /**
         * Sets the HTTP protocol to use (i.e. HTTP/1.1 or HTTP/2). Not all services support HTTP/2.
         *
         * @param protocol Protocol to use.
         * @return This builder for method chaining.
         */
        Builder protocol(Protocol protocol);

        /**
         * Sets the SSL related parameters (e.g. Protocols, CipherSuites, ApplicationProtocols etc.) via SSLParameters
         * object.
         *
         * @param sslParameter SSLParameters object.
         * @return This builder for method chaining.
         */
        Builder configureSsl(SSLParameters sslParameter);

        /**
         * Sets the proxy related configurations (e.g. URI, port number etc.).
         *
         * @param proxyConfiguration ProxyConfiguration object.
         * @return This builder for method chaining.
         */
        Builder proxyConfig(ProxyConfiguration proxyConfiguration);

        /**
         * Sets the amount of time to wait for a response before timeout.
         *
         * @param responseTimeout timeout duration.
         * @return This builder for method chaining.
         */
        Builder responseTimeout(Duration responseTimeout);


        /**
         * Sets the numberOfThreads that the executor can hold in the threads pool.
         *
         * @param numberOfThreads an integer as the number of threads will be used in the executor.
         * @return This builder for method chaining.
         */
        Builder numberOfThreads(Integer numberOfThreads);

        /**
         * Sets the executor in the HTTP Client as the executor created by users.
         *
         * @param executor a customized executor created by user
         * @return This builder for method chaining.
         */
        Builder requestExecutor(Executor executor);
    }

    private static final class DefaultBuilder implements Builder {

        private final AttributeMap.Builder standardOptions = AttributeMap.builder();

        /* private SSLParameters sslParameters; */

        private DefaultBuilder() {
        }

        @Override
        public Builder maxConcurrency(Integer maxConcurrency) {
            return null;
        }

        /**
         * Setter to set the MaxConcurrency directly.
         *
         * @param maxConcurrency New value for max concurrency.
         */
        public void setMaxConcurrency(Integer maxConcurrency) {}

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            return null;
        }

        /**
         * Setter to set the connection timeout directly.
         *
         * @param connectionTimeout timeout duration.
         */
        public void setConnectionTimeout(Duration connectionTimeout) {}

        @Override
        public Builder protocol(Protocol protocol) {
            return null;
        }

        /**
         * Setter to set the protocol of the http client.
         *
         * @param protocol Protocol to use.
         */
        public void setProtocol(Protocol protocol) {}

        @Override
        public Builder configureSsl(SSLParameters sslParameters) {
            return null;
        }

        @Override
        public Builder proxyConfig(ProxyConfiguration proxyconfiguration) {
            return null;
        }

        /**
         * Setter to pass the ProxyConfiguration object to Builder.
         *
         * @param proxyConfiguration ProxyConfiguration object.
         */
        public void setProxyConfig(ProxyConfiguration proxyConfiguration) {
        }

        @Override
        public Builder responseTimeout(Duration responseTimeout) {
            return null;
        }

        /**
         * Setter to set the timeout of waiting a response.
         *
         * @param responseTimeout timeout duration.
         */
        public void setResponseTimeout(Duration responseTimeout) {}

        @Override
        public Builder numberOfThreads(Integer numberOfThreads) {
            return null;
        }

        /**
         * Setter to set the number of threads in executor, configuring how many threads should the executor hold.
         *
         * @param numberOfThreads an integer as the number of threads will be used in the executor.
         * @param javaHttpRequestExecutor The executor of this HTTP Client that executes HTTP Requests.
         */
        public void setNumberOfThreads(Integer numberOfThreads, JavaHttpRequestExecutor javaHttpRequestExecutor) {}

        @Override
        public Builder requestExecutor(Executor executor) {
            return null;
        }

        @Override
        public SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            return new JavaNioAsyncHttpClient(this, standardOptions.build()
                    .merge(serviceDefaults)
                    .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));
        }
    }
}