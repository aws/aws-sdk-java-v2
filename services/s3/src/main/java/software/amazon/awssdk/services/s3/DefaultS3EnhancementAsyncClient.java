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

package software.amazon.awssdk.services.s3;

import java.net.MalformedURLException;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.protocols.xml.AwsS3ProtocolFactory;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.GetUrlResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Implementation of {@link S3EnhancementAsyncClient}.
 * These APIs are exposed through {@link S3AsyncClient} and {@link DefaultS3AsyncClient}.
 */
@SdkInternalApi
final class DefaultS3EnhancementAsyncClient implements S3EnhancementAsyncClient {

    private final S3EnhancementClient syncClient;

    DefaultS3EnhancementAsyncClient(BuilderImpl builder) {
        this.syncClient = DefaultS3EnhancementClient.builder()
                                                    .protocolFactory(builder.protocolFactory)
                                                    .sdkClientConfiguration(builder.clientConfiguration)
                                                    .build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link S3EnhancementAsyncClient}.
     */
    static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public CompletableFuture<GetUrlResponse> getUrl(GetUrlRequest getUrlRequest) throws MalformedURLException {
        try {
            GetUrlResponse response = syncClient.getUrl(getUrlRequest);

            CompletableFuture<GetUrlResponse> future = new CompletableFuture<>();
            future.complete(response);
            return future;
        } catch (Throwable t) {
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    public interface Builder {
        Builder protocolFactory(AwsS3ProtocolFactory protocolFactory);

        Builder sdkClientConfiguration(SdkClientConfiguration clientConfiguration);

        DefaultS3EnhancementAsyncClient build();
    }

    private static final class BuilderImpl implements Builder {
        private AwsS3ProtocolFactory protocolFactory;
        private SdkClientConfiguration clientConfiguration;

        @Override
        public Builder protocolFactory(AwsS3ProtocolFactory protocolFactory) {
            this.protocolFactory = protocolFactory;
            return this;
        }

        @Override
        public Builder sdkClientConfiguration(SdkClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
            return this;
        }

        @Override
        public DefaultS3EnhancementAsyncClient build() {
            return new DefaultS3EnhancementAsyncClient(this);
        }
    }
}
