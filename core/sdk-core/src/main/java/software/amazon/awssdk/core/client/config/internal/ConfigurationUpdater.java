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

package software.amazon.awssdk.core.client.config.internal;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;

/**
 * Updates the configuration builder using the given consumer.
 */
@SdkProtectedApi
@FunctionalInterface
public interface ConfigurationUpdater<T> {

    /**
     * Uses the given consumer to update a configuration.
     */
    SdkClientConfiguration update(Consumer<T> consumer, SdkClientConfiguration.Builder builder);

}
