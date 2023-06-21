/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.acm;

import java.net.URI;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.services.acm.endpoints.AcmEndpointProvider;

/**
 * Internal implementation of {@link AcmClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultAcmClientBuilder extends DefaultAcmBaseClientBuilder<AcmClientBuilder, AcmClient> implements AcmClientBuilder {
    @Override
    public DefaultAcmClientBuilder endpointProvider(AcmEndpointProvider endpointProvider) {
        clientConfiguration.option(SdkClientOption.ENDPOINT_PROVIDER, endpointProvider);
        return this;
    }

    @Override
    protected final AcmClient buildClient() {
        SdkClientConfiguration clientConfiguration = super.syncClientConfiguration();
        this.validateClientOptions(clientConfiguration);
        URI endpointOverride = null;
        if (clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN) != null
                && Boolean.TRUE.equals(clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN))) {
            endpointOverride = clientConfiguration.option(SdkClientOption.ENDPOINT);
        }
        AcmServiceClientConfiguration serviceClientConfiguration = AcmServiceClientConfiguration.builder()
                .overrideConfiguration(overrideConfiguration()).region(clientConfiguration.option(AwsClientOption.AWS_REGION))
                .endpointOverride(endpointOverride).build();
        return new DefaultAcmClient(serviceClientConfiguration, clientConfiguration);
    }
}
