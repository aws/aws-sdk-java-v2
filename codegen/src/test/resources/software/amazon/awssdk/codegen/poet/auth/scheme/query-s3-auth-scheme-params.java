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

package software.amazon.awssdk.services.s3.auth.scheme;

import java.util.Optional;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.auth.scheme.internal.DefaultS3AuthSchemeParams;

/**
 * The parameters object used to resolve the auth schemes for the S3 service.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface S3AuthSchemeParams {
    /**
     * Get a new builder for creating a {@link S3AuthSchemeParams}.
     */
    static Builder builder() {
        return DefaultS3AuthSchemeParams.builder();
    }

    /**
     * Returns the operation for which to resolve the auth scheme.
     */
    String operation();

    /**
     * Returns the region. The region is optional. The region parameter may be used with "aws.auth#sigv4" auth scheme.
     * By default, the region will be empty.
     */
    Optional<String> region();

    Boolean useDualStackEndpoint();

    Boolean useFipsEndpoint();

    String endpointId();

    Boolean defaultTrueParam();

    String defaultStringParam();

    String deprecatedParam();

    Boolean booleanContextParam();

    String stringContextParam();

    String operationContextParam();

    /**
     * A builder for a {@link S3AuthSchemeParams}.
     */
    interface Builder {
        /**
         * Set the operation for which to resolve the auth scheme.
         */
        Builder operation(String operation);

        /**
         * Set the region. The region parameter may be used with the "aws.auth#sigv4" auth scheme.
         */
        Builder region(String region);

        Builder useDualStackEndpoint(Boolean useDualStackEndpoint);

        Builder useFipsEndpoint(Boolean useFIPSEndpoint);

        Builder endpointId(String endpointId);

        Builder defaultTrueParam(Boolean defaultTrueParam);

        Builder defaultStringParam(String defaultStringParam);

        @Deprecated
        Builder deprecatedParam(String deprecatedParam);

        Builder booleanContextParam(Boolean booleanContextParam);

        Builder stringContextParam(String stringContextParam);

        Builder operationContextParam(String operationContextParam);

        /**
         * Returns a {@link S3AuthSchemeParams} object that is created from the properties that have been set on the
         * builder.
         */
        S3AuthSchemeParams build();
    }
}
