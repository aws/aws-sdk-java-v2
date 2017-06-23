/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.utils.AttributeMap;

/**
 * Interface for creating an {@link SdkHttpClient} with service specific defaults applied.
 *
 * <p>Implementations must be thread safe.</p>
 */
public interface SdkHttpClientFactory {

    /**
     * Create an {@link SdkHttpClient} with service specific defaults applied. Applying service defaults is optional
     * and some options may not be supported by a particular implementation.
     *
     * @param serviceDefaults Service specific defaults. Keys will be one of the constants defined in
     *                        {@link SdkHttpConfigurationOption}.
     * @return Created client
     */
    SdkHttpClient createHttpClientWithDefaults(AttributeMap serviceDefaults);
}
