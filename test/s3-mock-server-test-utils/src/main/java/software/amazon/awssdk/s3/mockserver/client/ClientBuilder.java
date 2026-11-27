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

package software.amazon.awssdk.s3.mockserver.client;

import java.util.function.Consumer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.utils.SdkAutoCloseable;

public class ClientBuilder {
    private static final StaticCredentialsProvider CREDS = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("AROA123456789EXAMPLE",
                    "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"));
    private static final Region DEFAULT_REGION = Region.US_WEST_2;

    private final S3ClientBuilder client;
    private final S3AsyncClientBuilder asyncClient;
    private final S3CrtAsyncClientBuilder crtAsyncClient;
    private final S3Configuration.Builder config;
    private final ClientOverrideConfiguration.Builder overrideConfig;
    private final int proxyPort;
    private boolean async;
    private boolean crtCreate;
    private HttpClient.Type httpType;
    private AwsCredentialsProvider credentialsOverride;
    private Region regionOverride;
    private boolean inMemoryTransport;
    private Consumer<SdkAutoCloseable> built = client -> { };

    public ClientBuilder(int proxyPort) {
        this.client = S3Client.builder();
        this.asyncClient = S3AsyncClient.builder();
        this.crtAsyncClient = S3AsyncClient.crtBuilder();
        this.config = S3Configuration.builder();
        this.overrideConfig = ClientOverrideConfiguration.builder();
        this.proxyPort = proxyPort;
    }

    public S3ClientBuilder client() {
        return client;
    }

    public S3AsyncClientBuilder asyncClient() {
        return asyncClient;
    }

    public S3Configuration.Builder config() {
        return config;
    }

    public ClientOverrideConfiguration.Builder overrideConfig() {
        return overrideConfig;
    }

    public int proxyPort() {
        return proxyPort;
    }

    public HttpClient.Type httpType() {
        return httpType;
    }

    public S3CrtAsyncClientBuilder crtClient() {
        return crtAsyncClient;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public boolean isCrtCreate() {
        return crtCreate;
    }

    public void setCrtCreate(boolean crtCreate) {
        this.crtCreate = crtCreate;
    }

    public void setHttpType(HttpClient.Type httpType) {
        this.httpType = httpType;
    }

    public boolean isInMemoryTransport() {
        return inMemoryTransport;
    }

    public void setInMemoryTransport(boolean inMemoryTransport) {
        this.inMemoryTransport = inMemoryTransport;
    }

    public ClientBuilder onBuilt(Consumer<SdkAutoCloseable> listener) {
        this.built = listener;
        return this;
    }

    public ClientKind kind() {
        if (crtCreate) {
            return ClientKind.CRT;
        }
        return async ? ClientKind.ASYNC : ClientKind.SYNC;
    }

    public ClientBuilder credentials(AwsCredentialsProvider provider) {
        this.credentialsOverride = provider;
        return this;
    }

    private AwsCredentialsProvider creds() {
        return credentialsOverride != null ? credentialsOverride : CREDS;
    }

    public ClientBuilder region(final Region region) {
        this.regionOverride = region;
        return this;
    }

    private Region region() {
        return regionOverride != null ? regionOverride : DEFAULT_REGION;
    }

    public S3Client buildSync() {
        client.region(region());
        client.credentialsProvider(creds());
        client.serviceConfiguration(config.build());
        client.overrideConfiguration(overrideConfig.build());
        S3Client built = client.build();
        this.built.accept(built);
        return built;
    }

    public S3AsyncClient buildAsync() {
        asyncClient.region(region());
        asyncClient.credentialsProvider(creds());
        asyncClient.serviceConfiguration(config.build());
        asyncClient.overrideConfiguration(overrideConfig.build());
        S3AsyncClient built = asyncClient.build();
        this.built.accept(built);
        return built;
    }

    public S3AsyncClient buildCrtAsync() {
        crtAsyncClient.region(region());
        crtAsyncClient.credentialsProvider(creds());
        S3AsyncClient built = crtAsyncClient.build();
        this.built.accept(built);
        return built;
    }
}

