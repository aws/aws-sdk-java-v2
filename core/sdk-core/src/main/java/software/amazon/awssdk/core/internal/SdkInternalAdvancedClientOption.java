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

package software.amazon.awssdk.core.internal;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientOption;

/**
 * Options of {@link SdkAdvancedClientOption} that <b>must not be used</b> outside of internals of this project. Changes to this
 * class <b>are not guaranteed to be backwards compatible</b>.
 */
@SdkInternalApi
public class SdkInternalAdvancedClientOption<T> extends SdkAdvancedClientOption<T> {
    /**
     * The endpoint override is not currently reflected in the client options. We set its value using this internal option to
     * allow to reconstruct the {@link SdkServiceClientConfiguration} with the same set of values without having to branch on
     * whether the {@link SdkClientOption#ENDPOINT_OVERRIDDEN} was set.
     */
    @SdkInternalApi
    public static final SdkInternalAdvancedClientOption<URI> ENDPOINT_OVERRIDE_VALUE =
        new SdkInternalAdvancedClientOption<>(URI.class);

    protected SdkInternalAdvancedClientOption(Class<T> valueClass) {
        super(valueClass);
    }
}
