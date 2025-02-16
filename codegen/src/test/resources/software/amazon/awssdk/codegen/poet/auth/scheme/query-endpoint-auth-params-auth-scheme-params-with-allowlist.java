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

package software.amazon.awssdk.services.query.auth.scheme;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.query.auth.scheme.internal.DefaultQueryAuthSchemeParams;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The parameters object used to resolve the auth schemes for the Query service.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface QueryAuthSchemeParams extends ToCopyableBuilder<QueryAuthSchemeParams.Builder, QueryAuthSchemeParams> {
    /**
     * Get a new builder for creating a {@link QueryAuthSchemeParams}.
     */
    static Builder builder() {
        return DefaultQueryAuthSchemeParams.builder();
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
     * Returns the RegionSet. The regionSet parameter may be used with the "aws.auth#sigv4a" auth scheme.
     */
    RegionSet regionSet();

    /**
     * A param that defauls to true
     */
    Boolean defaultTrueParam();

    String defaultStringParam();

    @Deprecated
    String deprecatedParam();

    Boolean booleanContextParam();

    String stringContextParam();

    String operationContextParam();

    /**
     * Returns a {@link Builder} to customize the parameters.
     */
    Builder toBuilder();

    /**
     * A builder for a {@link QueryAuthSchemeParams}.
     */
    interface Builder extends CopyableBuilder<Builder, QueryAuthSchemeParams> {
        /**
         * Set the operation for which to resolve the auth scheme.
         */
        Builder operation(String operation);

        /**
         * Set the region. The region parameter may be used with the "aws.auth#sigv4" auth scheme.
         */
        Builder region(Region region);

        /**
         * Set the RegionSet. The regionSet parameter may be used with the "aws.auth#sigv4a" auth scheme.
         */
        Builder regionSet(RegionSet regionSet);

        /**
         * A param that defauls to true
         */
        Builder defaultTrueParam(Boolean defaultTrueParam);

        Builder defaultStringParam(String defaultStringParam);

        @Deprecated
        Builder deprecatedParam(String deprecatedParam);

        Builder booleanContextParam(Boolean booleanContextParam);

        Builder stringContextParam(String stringContextParam);

        Builder operationContextParam(String operationContextParam);

        /**
         * Returns a {@link QueryAuthSchemeParams} object that is created from the properties that have been set on the
         * builder.
         */
        QueryAuthSchemeParams build();
    }
}
