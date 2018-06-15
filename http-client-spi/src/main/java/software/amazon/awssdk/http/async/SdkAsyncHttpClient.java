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

package software.amazon.awssdk.http.async;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.ConfigurationProvider;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Interface to take a representation of an HTTP request, asynchronously make an HTTP call, and return a representation of an
 * HTTP response.
 *
 * <p>Implementations MUST be thread safe.</p>
 *
 * <p><b><i>Note: This interface will change between SDK versions and should not be implemented by SDK users.</i></b></p>
 */
@Immutable
@ThreadSafe
@SdkProtectedApi
public interface SdkAsyncHttpClient extends SdkAutoCloseable, ConfigurationProvider {
    /**
     * Create an {@link AbortableRunnable} that can be used to execute the HTTP request.
     *
     * @param request         HTTP request (without content).
     * @param context         Request context containing additional dependencies.
     * @param requestProvider Representation of an HTTP requestProvider.
     * @param handler         The handler that will be called when data is received.
     * @return Task that can execute an HTTP requestProvider and can be aborted.
     */
    @ReviewBeforeRelease("Should we wrap this in a container for more flexibility?")
    AbortableRunnable prepareRequest(SdkHttpRequest request,
                                     SdkRequestContext context,
                                     SdkHttpRequestProvider requestProvider,
                                     SdkHttpResponseHandler handler);

    /**
     * Interface for creating an {@link SdkAsyncHttpClient} with service specific defaults applied.
     *
     * <p>Implementations must be thread safe.</p>
     */
    @FunctionalInterface
    interface Builder {
        /**
         * Create a {@link SdkAsyncHttpClient} without defaults applied. This is useful for reusing an HTTP client across multiple
         * services.
         */
        default SdkAsyncHttpClient build() {
            return buildWithDefaults(AttributeMap.empty());
        }

        /**
         * Create an {@link SdkAsyncHttpClient} with service specific defaults applied. Applying service defaults is optional
         * and some options may not be supported by a particular implementation.
         *
         * @param serviceDefaults Service specific defaults. Keys will be one of the constants defined in
         *                        {@link SdkHttpConfigurationOption}.
         * @return Created client
         */
        SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults);
    }
}
