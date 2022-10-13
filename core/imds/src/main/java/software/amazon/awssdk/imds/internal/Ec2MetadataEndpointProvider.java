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

package software.amazon.awssdk.imds.internal;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.imds.EndpointMode;
import software.amazon.awssdk.profiles.ProfileFile;

/**
 * Provides the logic to determine which endpoint and {@link EndpointMode} to use for retrieving the instance metadata.
 */
@SdkInternalApi
public interface Ec2MetadataEndpointProvider {

    /**
     * Resolve an endpoint, based on the specified {@link EndpointMode}. Encapsulate the logic for determining which endpoint
     * to use.
     * @param endpointMode the endpoint mode to use to get an endpoint
     * @return A string representing the uri of the endpoint
     */
    String resolveEndpoint(EndpointMode endpointMode);

    /**
     * Encapsulate the logic for determining which Endpoint mode to use.
     * @return the {@link EndpointMode} to use for the metadata endpoint.
     */
    EndpointMode resolveEndpointMode();

    interface Builder {

        /**
         * @param profileFile the AWS profile file to use.
         * @return this builder instance
         */
        Builder profileFile(Supplier<ProfileFile> profileFile);

        /**
         * @param profileName the AWS profile name to use.
         * @return this builder instance.
         */
        Builder profileName(String profileName);

        Ec2MetadataEndpointProvider build();
    }

}
