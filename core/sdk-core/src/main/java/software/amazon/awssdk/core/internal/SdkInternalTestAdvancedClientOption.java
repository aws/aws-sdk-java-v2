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
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientOption;

/**
 * Options of {@link SdkAdvancedClientOption} that <b>must not be used</b> outside of tests that are stored in this project.
 * Changes to this class <b>are not guaranteed to be backwards compatible</b>.
 */
@SdkInternalApi
public class SdkInternalTestAdvancedClientOption<T> extends SdkAdvancedClientOption<T> {
    /**
     * By default, the SDK handles endpoints specified via {@link SdkClientBuilder#endpointOverride(URI)} differently than
     * endpoints generated from a specific region. For example, endpoint discovery is not supported in some cases when endpoint
     * overrides are used.
     *
     * When this option is set, the {@link SdkClientOption#ENDPOINT_OVERRIDDEN} is forced to this value. Because of the way this
     * is implemented, the client configuration must be configured *after* the {@code endpointOverride} is configured.
     */
    @SdkTestInternalApi
    public static final SdkInternalTestAdvancedClientOption<Boolean> ENDPOINT_OVERRIDDEN_OVERRIDE =
        new SdkInternalTestAdvancedClientOption<>(Boolean.class);

    protected SdkInternalTestAdvancedClientOption(Class<T> valueClass) {
        super(valueClass);
    }
}
