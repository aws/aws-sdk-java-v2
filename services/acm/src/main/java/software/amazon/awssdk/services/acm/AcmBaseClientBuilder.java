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

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.services.acm.auth.scheme.AcmAuthSchemeProvider;
import software.amazon.awssdk.services.acm.endpoints.AcmEndpointProvider;

/**
 * This includes configuration specific to ACM that is supported by both {@link AcmClientBuilder} and
 * {@link AcmAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
public interface AcmBaseClientBuilder<B extends AcmBaseClientBuilder<B, C>, C> extends AwsClientBuilder<B, C> {
    /**
     * Set the {@link AcmEndpointProvider} implementation that will be used by the client to determine the endpoint for
     * each request. This is optional; if none is provided a default implementation will be used the SDK.
     */
    default B endpointProvider(AcmEndpointProvider endpointProvider) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the {@link AcmAuthSchemeProvider} implementation that will be used by the client to resolve the auth scheme
     * for each request. This is optional; if none is provided a default implementation will be used the SDK.
     */
    default B authSchemeProvider(AcmAuthSchemeProvider authSchemeProvider) {
        throw new UnsupportedOperationException();
    }
}
