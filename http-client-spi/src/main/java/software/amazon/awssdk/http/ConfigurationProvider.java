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

import java.util.Optional;

/**
 * Interface to provide read only access to the configuration of the {@link SdkHttpClient}.
 */
public interface ConfigurationProvider {

    /**
     * Retrieve the current value of the configuration option, if present.
     *
     * @param key Key of configuration value to retrieve.
     * @param <T> Type of configuration value.
     * @return Empty {@link java.util.Optional} if configuration option is not supported, otherwise a fulfilled {@link
     * java.util.Optional} containing the current value.
     */
    <T> Optional<T> getConfigurationValue(SdkHttpConfigurationOption<T> key);
}
