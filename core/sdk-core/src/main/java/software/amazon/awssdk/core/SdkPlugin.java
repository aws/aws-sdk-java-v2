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

package software.amazon.awssdk.core;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * A plugin modifies a client's configuration when the client is created or at request execution
 * time.
 */
@SdkPublicApi
@ThreadSafe
@FunctionalInterface
public interface SdkPlugin extends SdkAutoCloseable {

    /**
     * Modify the provided client configuration.
     */
    void configureClient(SdkServiceClientConfiguration.Builder config);

    @Override
    default void close() {
    }
}
