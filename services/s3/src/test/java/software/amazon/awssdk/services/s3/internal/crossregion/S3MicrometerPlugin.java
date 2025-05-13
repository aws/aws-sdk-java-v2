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

package software.amazon.awssdk.services.s3.internal.crossregion;

import io.micrometer.observation.ObservationRegistry;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;

public class S3MicrometerPlugin implements SdkPlugin {
    ObservationRegistry observationRegistry;
    private S3MicrometerPlugin(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    public static SdkPlugin create(ObservationRegistry observationRegistry) {
        return new S3MicrometerPlugin(observationRegistry);
    }

    @Override
    public void configureClient(SdkServiceClientConfiguration.Builder config) {
        if (!(config instanceof S3ServiceClientConfiguration.Builder)) {
            return;
        }
        S3ServiceClientConfiguration.Builder s3Config = (S3ServiceClientConfiguration.Builder) config;
        s3Config.overrideConfiguration(s3Config.overrideConfiguration()
                                               .toBuilder()
                                               .addExecutionInterceptor(new ObservingExecutionInterceptor(observationRegistry))
                                               .build());
    }
}
