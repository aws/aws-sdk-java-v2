/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.awscore.internal;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoint.DefaultServiceEndpointBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Validate;

/**
 * A collection of utility methods centered around generating service endpoints from various pieces of information.
 */
@SdkInternalApi
public final class EndpointUtils {
    private EndpointUtils() {}

    /**
     * Generate an endpoint from the provided endpoint protocol, url prefix, and region.
     *
     * @param protocol The protocol that should be used when communicating with AWS (usually http or https).
     * @param serviceEndpointPrefix The endpoint prefix that should be used when communicating with AWS (usually the
     *                              endpointPrefix in the service's model).
     * @param region The AWS region that should be communicated with.
     * @return The AWS endpoint to use for communication.
     */
    public static URI buildEndpoint(String protocol, String serviceEndpointPrefix, Region region) {
        Validate.paramNotNull(protocol, "protocol");
        Validate.paramNotNull(serviceEndpointPrefix, "serviceEndpointPrefix");
        Validate.paramNotNull(region, "region");
        return new DefaultServiceEndpointBuilder(serviceEndpointPrefix, protocol).withRegion(region).getServiceEndpoint();
    }
}
