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

package software.amazon.awssdk.services.codecatalyst.auth.scheme;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.codecatalyst.auth.scheme.internal.DefaultCodeCatalystAuthSchemeParams;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The parameters object used to resolve the auth schemes for the CodeCatalyst service.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface CodeCatalystAuthSchemeParams extends
        ToCopyableBuilder<CodeCatalystAuthSchemeParams.Builder, CodeCatalystAuthSchemeParams> {
    /**
     * Get a new builder for creating a {@link CodeCatalystAuthSchemeParams}.
     */
    static Builder builder() {
        return DefaultCodeCatalystAuthSchemeParams.builder();
    }

    /**
     * Returns the operation for which to resolve the auth scheme.
     */
    String operation();

    /**
     * Returns a {@link Builder} to customize the parameters.
     */
    Builder toBuilder();

    /**
     * A builder for a {@link CodeCatalystAuthSchemeParams}.
     */
    interface Builder extends CopyableBuilder<Builder, CodeCatalystAuthSchemeParams> {
        /**
         * Set the operation for which to resolve the auth scheme.
         */
        Builder operation(String operation);

        /**
         * Returns a {@link CodeCatalystAuthSchemeParams} object that is created from the properties that have been set
         * on the builder.
         */
        CodeCatalystAuthSchemeParams build();
    }
}
