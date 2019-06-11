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
import software.amazon.awssdk.utils.Validate;


/**
 * An implementation of {@link SdkAsyncHttpClient} that uses a Java HTTP Client.
 *
 * <p>This can be created via {@link @builder()}</p>
 */
@SdkPublicApi
public final class JavaHttpClientNioAsyncHttpClient implements SdkAsyncHttpClient {

    private static final String CLIENT_NAME = "JavaNio";
    private final HttpClient javaHttpClient;
    /*private final JavaHttpClientConfiguration configuration;*/
    private final AttributeMap serviceDefaultsMap;

    private JavaHttpClientNioAsyncHttpClient(DefaultBuilder builder, AttributeMap serviceDefaultsMap) {
        /*this.configuration = new JavaHttpClientConfiguration(serviceDefaultsMap);*/
        this.javaHttpClient = HttpClient.newBuilder()
                .connectTimeout(serviceDefaultsMap.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT))
                .version(convertProtocolIntoVersion(serviceDefaultsMap.get(SdkHttpConfigurationOption.PROTOCOL)))
                .sslParameters(serviceDefaultsMap.get(SdkHttpConfigurationOption.SSL_PARAMETERS))
                .proxy(builder.sdkProxyConfig.getProxySelector())
                .authenticator(builder.sdkProxyConfig.getAuthenticator())
                .build();
        this.serviceDefaultsMap = serviceDefaultsMap;
    }

    protected HttpClient getHttpClient() {
        return javaHttpClient;
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
        // TODO: Change the implementation into handle single executor for a certain HttpClient
        JavaHttpRequestExecutor javaHttpRequestExecutor = new JavaHttpRequestExecutor(javaHttpClient, serviceDefaultsMap);
        return javaHttpRequestExecutor.requestExecution(request);
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }


    public void close() {
    }

    private HttpClient.Version convertProtocolIntoVersion(Protocol protocol) {
        return protocol == Protocol.HTTP1_1 ? HttpClient.Version.HTTP_1_1 : HttpClient.Version.HTTP_2;
    }

    @Override
    public String clientName() {
        return CLIENT_NAME;
    }


    /**
     * Builder that allows configuration of the Java NIO HTTP implementation. Use {@link #builder()} to configure
     * and construct a Java Http Client.
     */
    public interface Builder extends SdkAsyncHttpClient.Builder<JavaHttpClientNioAsyncHttpClient.Builder> {

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
         * @param sdkProxyConfig  object.
         * @return This builder for method chaining.
         */
        Builder proxyConfig(SdkProxyConfig sdkProxyConfig);

        /**
         * Sets the amount of time to wait for a response before timeout.
         *
         * @param responseTimeout timeout duration.
         * @return This builder for method chaining.
         */
        Builder responseTimeout(Duration responseTimeout);

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
        private SdkProxyConfig sdkProxyConfig = SdkProxyConfig.builder().build();

        private DefaultBuilder() {
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            Validate.isPositive(connectionTimeout, "connectionTimeout");
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, connectionTimeout);
            return this;
        }

        /**
         * Setter to set the connection timeout directly.
         *
         * @param connectionTimeout timeout duration.
         */
        public void setConnectionTimeout(Duration connectionTimeout) {
            connectionTimeout(connectionTimeout);
        }

        @Override
        public Builder protocol(Protocol protocol) {
            standardOptions.put(SdkHttpConfigurationOption.PROTOCOL, protocol);
            return this;
        }

        /**
         * Setter to set the protocol of the http client.
         *
         * @param protocol Protocol to use.
         */
        public void setProtocol(Protocol protocol) {
            protocol(protocol);
        }

        @Override
        public Builder configureSsl(SSLParameters sslParameters) {
            standardOptions.put(SdkHttpConfigurationOption.SSL_PARAMETERS, sslParameters);
            return this;
        }

        @Override
        public Builder proxyConfig(SdkProxyConfig sdkProxyConfig) {
            this.sdkProxyConfig = sdkProxyConfig;
            return this;
        }

        /**
         * Setter to pass the sdkProxyConfig object to Builder.
         *
         * @param sdkProxyConfig sdkProxyConfig object.
         */
        public void setProxyConfig(SdkProxyConfig sdkProxyConfig) {
            proxyConfig(sdkProxyConfig);
        }

        @Override
        public Builder responseTimeout(Duration responseTimeout) {
            standardOptions.put(SdkHttpConfigurationOption.RESPONSE_TIMEOUT, responseTimeout);
            return this;
        }

        /**
         * Setter to set the timeout of waiting a response.
         *
         * @param responseTimeout timeout duration.
         */
        public void setResponseTimeout(Duration responseTimeout) {
            responseTimeout(responseTimeout);
        }

        /**
         * If customers use this method then we should not close the executor when the
         * client is closed.
         * @param executor a customized executor created by user
         * @return
         */
        @Override
        public Builder requestExecutor(Executor executor) {
            return this;
        }

        @Override
        public SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            return new JavaHttpClientNioAsyncHttpClient(this, standardOptions.build()
                                                                        .merge(serviceDefaults)
                                                                        .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));
        }
    }
}