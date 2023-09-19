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
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;

/**
 * An instance of this class is stored in a {@link SdkClientOption} to allow the service specific
 * {@link SdkServiceClientConfiguration} to be updated and converted back to a {@link SdkClientConfiguration} instance.
 */
@SdkProtectedApi
@FunctionalInterface
public interface InternalizeExternalConfiguration {

    /**
     * Uses the given consumer to update a service-specific subclass of {@link SdkClientConfiguration} and convert back the
     * result to a {@code SdkClientConfiguration} instance.
     */
    SdkClientConfiguration updateUsing(Consumer<? super SdkServiceClientConfiguration.Builder> externalBuilder,
                                       SdkClientConfiguration.Builder internalBuilder);

}
