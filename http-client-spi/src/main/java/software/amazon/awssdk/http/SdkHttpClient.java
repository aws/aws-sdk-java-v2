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

package software.amazon.awssdk.http;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Interface to take a representation of an HTTP request, make an HTTP call, and return a representation of an HTTP response.
 *
 * <p>Implementations MUST be thread safe.</p>
 *
 * <p><b><i>Note: This interface will change between SDK versions and should not be implemented by SDK users.</i></b></p>
 */
@Immutable
@ThreadSafe
@SdkProtectedApi
public interface SdkHttpClient extends SdkAutoCloseable, ConfigurationProvider {
    /**
     * Create a {@link AbortableCallable} that can be used to execute the HTTP request.
     *
     * @param request        Representation of an HTTP request.
     * @param requestContext Contains any extra dependencies needed.
     * @return Task that can execute an HTTP request and can be aborted.
     */
    AbortableCallable<SdkHttpFullResponse> prepareRequest(SdkHttpFullRequest request, SdkRequestContext requestContext);

    /**
     * Interface for creating an {@link SdkHttpClient} with service specific defaults applied.
     */
    @FunctionalInterface
    interface Builder<T extends SdkHttpClient.Builder<T>> extends SdkBuilder<T, SdkHttpClient> {
        /**
         * Create a {@link SdkHttpClient} without defaults applied. This is useful for reusing an HTTP client across multiple
         * services.
         */
        default SdkHttpClient build() {
            return buildWithDefaults(AttributeMap.empty());
        }

        /**
         * Create an {@link SdkHttpClient} with service specific defaults applied. Applying service defaults is optional
         * and some options may not be supported by a particular implementation.
         *
         * @param serviceDefaults Service specific defaults. Keys will be one of the constants defined in
         *                        {@link SdkHttpConfigurationOption}.
         * @return Created client
         */
        SdkHttpClient buildWithDefaults(AttributeMap serviceDefaults);
    }
}
