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

package software.amazon.awssdk.services.acm.auth.scheme;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.acm.auth.scheme.internal.DefaultAcmAuthSchemeParams;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The parameters object used to resolve the auth schemes for the Acm service.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface AcmAuthSchemeParams extends ToCopyableBuilder<AcmAuthSchemeParams.Builder, AcmAuthSchemeParams> {
    /**
     * Get a new builder for creating a {@link AcmAuthSchemeParams}.
     */
    static Builder builder() {
        return DefaultAcmAuthSchemeParams.builder();
    }

    /**
     * Returns the operation for which to resolve the auth scheme.
     */
    String operation();

    /**
     * Returns the region. The region parameter may be used with the "aws.auth#sigv4" auth scheme.
     */
    Region region();

    /**
     * Returns a {@link Builder} to customize the parameters.
     */
    Builder toBuilder();

    /**
     * A builder for a {@link AcmAuthSchemeParams}.
     */
    interface Builder extends CopyableBuilder<Builder, AcmAuthSchemeParams> {
        /**
         * Set the operation for which to resolve the auth scheme.
         */
        Builder operation(String operation);

        /**
         * Set the region. The region parameter may be used with the "aws.auth#sigv4" auth scheme.
         */
        Builder region(Region region);

        /**
         * Returns a {@link AcmAuthSchemeParams} object that is created from the properties that have been set on the
         * builder.
         */
        AcmAuthSchemeParams build();
    }
}
