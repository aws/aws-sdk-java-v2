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

package software.amazon.awssdk.s3.wiretap.client.feature.transport;

import software.amazon.awssdk.s3.wiretap.client.feature.AbstractFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.Category;

public final class MultipartFeature extends AbstractFeature<MultipartFeature.Config> {

    public record Config(long threshold, long partSize) { }

    public MultipartFeature() {
        super("mpu", Config.class);
        onAsync((b, config) -> b.asyncClient()
                .multipartEnabled(true)
                .multipartConfiguration(c -> c
                        .thresholdInBytes(config.threshold())
                        .minimumPartSizeInBytes(config.partSize())));
    }

    @Override
    public Category<Config> defaultCategory(Class<?> paramType) {
        return new Disabled();
    }

    public static final class EnabledDefault implements Category<Config> {
        private static final long EIGHT_MIB = 8L * 1024 * 1024;

        @Override
        public Config value() {
            return new Config(EIGHT_MIB, EIGHT_MIB);
        }
    }

    public static final class Disabled implements Category<Config> {
        @Override
        public Config value() {
            return null;
        }
    }
}
