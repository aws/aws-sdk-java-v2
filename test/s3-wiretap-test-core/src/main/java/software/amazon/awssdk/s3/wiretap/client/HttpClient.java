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

package software.amazon.awssdk.s3.wiretap.client;

import java.net.URI;
import lombok.Getter;
import lombok.experimental.Accessors;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.crt.S3CrtHttpConfiguration;
import software.amazon.awssdk.services.s3.crt.S3CrtProxyConfiguration;
import software.amazon.awssdk.utils.AttributeMap;

public final class HttpClient {

    @Accessors(fluent = true)
    public enum Type {
        APACHE("md/http#Apache ", false),
        APACHE5("md/http#Apache5", false),
        URL_CONNECTION("md/http#UrlConnection", false),
        AWS_CRT("md/http#AwsCommonRuntime", false),
        NETTY_NIO("md/http#NettyNio", true),
        AWS_CRT_ASYNC("md/http#AwsCommonRuntime", true),
        CRT_CREATE("md/http#s3crt", true);

        @Getter
        private final String userAgentToken;
        private final boolean async;

        Type(String userAgentToken, boolean async) {
            this.userAgentToken = userAgentToken;
            this.async = async;
        }

        public boolean isAsync() {
            return async;
        }

        public ClientKind kind() {
            if (this == CRT_CREATE) {
                return ClientKind.CRT;
            }
            return async ? ClientKind.ASYNC : ClientKind.SYNC;
        }
    }

    private HttpClient() {
    }

    public static void apply(ClientBuilder builder, Type type) {
        builder.setHttpType(type);
        if (type == Type.CRT_CREATE) {
            builder.setAsync(true);
            builder.setCrtCreate(true);
            builder.crtClient().httpConfiguration(
                    S3CrtHttpConfiguration.builder()
                            .trustAllCertificatesEnabled(true)
                            .proxyConfiguration(S3CrtProxyConfiguration.builder()
                                    .host("localhost")
                                    .port(builder.proxyPort())
                                    .scheme("http")
                                    .useSystemPropertyValues(false)
                                    .build())
                            .build());
        } else if (type.isAsync()) {
            builder.setAsync(true);
            builder.asyncClient().httpClient(buildAsyncClient(type, builder.proxyPort()));
        } else {
            builder.client().httpClient(buildClient(type, builder.proxyPort()));
        }
    }

    private static SdkHttpClient buildClient(Type type, int proxyPort) {
        AttributeMap attributeMap =
                AttributeMap.builder()
                        .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                        .build();
        switch (type) {
            case APACHE:
                return ApacheHttpClient.builder()
                        .proxyConfiguration(software.amazon.awssdk.http.apache.ProxyConfiguration.builder()
                                .endpoint(URI.create("http://localhost:" + proxyPort))
                                .useSystemPropertyValues(false)
                                .build())
                        .buildWithDefaults(attributeMap);
            case APACHE5:
                return Apache5HttpClient.builder()
                        .proxyConfiguration(software.amazon.awssdk.http.apache5.ProxyConfiguration.builder()
                                .endpoint(URI.create("http://localhost:" + proxyPort))
                                .useSystemPropertyValues(false)
                                .build())
                        .buildWithDefaults(attributeMap);
            case URL_CONNECTION:
                return UrlConnectionHttpClient.builder()
                        .proxyConfiguration(software.amazon.awssdk.http.urlconnection.ProxyConfiguration.builder()
                                .endpoint(URI.create("http://localhost:" + proxyPort))
                                .useSystemPropertyValues(false)
                                .build())
                        .buildWithDefaults(attributeMap);
            case AWS_CRT:
                return AwsCrtHttpClient.builder()
                        .proxyConfiguration(software.amazon.awssdk.http.crt.ProxyConfiguration.builder()
                                .host("localhost")
                                .port(proxyPort)
                                .useSystemPropertyValues(false)
                                .build())
                        .buildWithDefaults(attributeMap);
            default:
                throw new IllegalArgumentException("Not a sync HTTP client type: " + type);
        }
    }

    private static SdkAsyncHttpClient buildAsyncClient(Type type, int proxyPort) {
        AttributeMap attributeMap =
                AttributeMap.builder()
                        .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                        .build();
        return switch (type) {
            case NETTY_NIO -> NettyNioAsyncHttpClient.builder()
                    .proxyConfiguration(software.amazon.awssdk.http.nio.netty.ProxyConfiguration.builder()
                            .host("localhost")
                            .port(proxyPort)
                            .scheme("http")
                            .build())
                    .buildWithDefaults(attributeMap);
            case AWS_CRT_ASYNC -> AwsCrtAsyncHttpClient.builder()
                    .proxyConfiguration(software.amazon.awssdk.http.crt.ProxyConfiguration.builder()
                            .host("localhost")
                            .port(proxyPort)
                            .useSystemPropertyValues(false)
                            .build())
                    .buildWithDefaults(attributeMap);
            default -> throw new IllegalArgumentException("Not an async HTTP client type: " + type);
        };
    }
}
