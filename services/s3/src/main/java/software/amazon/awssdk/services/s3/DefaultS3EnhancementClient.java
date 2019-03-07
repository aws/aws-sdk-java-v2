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
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.protocols.xml.AwsS3ProtocolFactory;
import software.amazon.awssdk.services.s3.internal.handlers.EndpointAddressInterceptor;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.GetUrlResponse;
import software.amazon.awssdk.services.s3.transform.GetUrlRequestMarshaller;

/**
 * Implementation of {@link S3EnhancementClient}.
 * These APIs are exposed through {@link S3Client} and {@link DefaultS3Client}.
 */
@SdkInternalApi
final class DefaultS3EnhancementClient implements S3EnhancementClient {

    private final AwsS3ProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private DefaultS3EnhancementClient(BuilderImpl builder) {
        this.protocolFactory = builder.protocolFactory;
        this.clientConfiguration = builder.clientConfiguration;
    }

    /**
     * Create a builder that can be used to configure and create a {@link S3EnhancementClient}.
     */
    static DefaultS3EnhancementClient.Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public GetUrlResponse getUrl(GetUrlRequest getUrlRequest) throws MalformedURLException {
        SdkHttpFullRequest marshalledRequest = new GetUrlRequestMarshaller(protocolFactory)
            .marshall(getUrlRequest);

        SdkHttpRequest requestAfterInterceptor = applyEndpointInterceptor(marshalledRequest, getUrlRequest);

        return GetUrlResponse.builder()
                             .url(requestAfterInterceptor.getUri().toURL())
                             .build();
    }

    private SdkHttpRequest applyEndpointInterceptor(SdkHttpFullRequest httpFullRequest, GetUrlRequest getUrlRequest) {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SERVICE_CONFIG,
                                         clientConfiguration.option(SdkClientOption.SERVICE_CONFIGURATION));
        executionAttributes.putAttribute(AwsExecutionAttribute.AWS_REGION,
                                         clientConfiguration.option(AwsClientOption.AWS_REGION));

        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();
        return interceptor.modifyHttpRequest(constructModifyHttpRequest(httpFullRequest, getUrlRequest),
                                             executionAttributes);
    }

    private static Context.ModifyHttpRequest constructModifyHttpRequest(SdkHttpFullRequest httpFullRequest,
                                                                        GetUrlRequest getUrlRequest) {
        return new Context.ModifyHttpRequest() {
            @Override
            public SdkHttpRequest httpRequest() {
                return httpFullRequest;
            }

            @Override
            public Optional<RequestBody> requestBody() {
                return Optional.empty();
            }

            @Override
            public Optional<AsyncRequestBody> asyncRequestBody() {
                return Optional.empty();
            }

            @Override
            public SdkRequest request() {
                return getUrlRequest;
            }
        };
    }

    public interface Builder {
        Builder protocolFactory(AwsS3ProtocolFactory protocolFactory);

        Builder sdkClientConfiguration(SdkClientConfiguration clientConfiguration);

        DefaultS3EnhancementClient build();
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
        public DefaultS3EnhancementClient build() {
            return new DefaultS3EnhancementClient(this);
        }
    }
}
