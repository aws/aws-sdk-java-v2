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

package software.amazon.awssdk.http.async;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
* Interface to take a representation of an HTTP request, asynchronously make an HTTP call, and return a representation of an
* HTTP response.
*
* <p>Implementations MUST be thread safe.</p>
*/
@Immutable
@ThreadSafe
@SdkPublicApi
public interface SdkAsyncHttpClient extends SdkAutoCloseable {

    /**
     * Execute the request.
     *
     * @param request The request object.
     *
     * @return The future holding the result of the request execution. Upon success execution of the request, the future is
     * completed with {@code null}, otherwise it is completed exceptionally.
     */
    CompletableFuture<Void> execute(AsyncExecuteRequest request);

    /**
     * Each HTTP client implementation should return a well-formed client name
     * that allows requests to be identifiable back to the client that made the request.
     * The client name should include the backing implementation as well as the Sync or Async
     * to identify the transmission type of the request. Client names should only include
     * alphanumeric characters. Examples of well formed client names include, Apache, for
     * requests using Apache's http client or NettyNio for Netty's http client.
     *
     * @return String containing the name of the client
     */
    default String clientName() {
        return "UNKNOWN";
    }

    @FunctionalInterface
    interface Builder<T extends SdkAsyncHttpClient.Builder<T>> extends SdkBuilder<T, SdkAsyncHttpClient> {
        /**
         * Create a {@link SdkAsyncHttpClient} with global defaults applied. This is useful for reusing an HTTP client across
         * multiple services.
         */
        default SdkAsyncHttpClient build() {
            return buildWithDefaults(AttributeMap.empty());
        }

        /**
         * Create an {@link SdkAsyncHttpClient} with service specific defaults applied. Applying service defaults is optional
         * and some options may not be supported by a particular implementation.
         *
         * @param serviceDefaults Service specific defaults. Keys will be one of the constants defined in {@link
         *                        SdkHttpConfigurationOption}.
         * @return Created client
         */
        SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults);
    }
}
