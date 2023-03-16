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
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;

/**
 * Class to expose SDK service client settings to the user, e.g., ClientOverrideConfiguration
 */
@SdkPublicApi
public class SdkServiceClientConfiguration {

    private final ClientOverrideConfiguration overrideConfiguration;

    public SdkServiceClientConfiguration(ClientOverrideConfiguration clientOverrideConfiguration) {
        this.overrideConfiguration = clientOverrideConfiguration;
    }

    /**
     *
     * @return The ClientOverrideConfiguration of the SdkClient. If this is not set, an ClientOverrideConfiguration object will
     * still be returned, with empty fields
     */
    public ClientOverrideConfiguration overrideConfiguration() {
        return this.overrideConfiguration;
    }
}
