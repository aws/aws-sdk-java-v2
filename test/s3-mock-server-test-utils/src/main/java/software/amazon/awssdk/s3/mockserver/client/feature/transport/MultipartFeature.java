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

package software.amazon.awssdk.s3.mockserver.client.feature.transport;

import java.util.Objects;
import software.amazon.awssdk.s3.mockserver.client.feature.AbstractFeature;
import software.amazon.awssdk.s3.mockserver.client.feature.Category;

public final class MultipartFeature extends AbstractFeature<MultipartFeature.Config> {

    public static final class Config {
        private final long threshold;
        private final long partSize;

        public Config(long threshold, long partSize) {
            this.threshold = threshold;
            this.partSize = partSize;
        }

        public long threshold() {
            return threshold;
        }

        public long partSize() {
            return partSize;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Config that = (Config) o;
            return threshold == that.threshold
                    && partSize == that.partSize;
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(threshold);
            result = 31 * result + Objects.hashCode(partSize);
            return result;
        }

        @Override
        public String toString() {
            return "Config[threshold=" + threshold + ", partSize=" + partSize + "]";
        }
    }

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
