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

package software.amazon.awssdk.s3.wiretap.client.feature.crt;

import software.amazon.awssdk.s3.wiretap.client.feature.AbstractFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.Category;

public final class MinimumPartSizeFeature extends AbstractFeature<Long> {

    private static final long MIB = 1024L * 1024L;

    public MinimumPartSizeFeature() {
        super("minPartSize", Long.class);
        onCrt((b, v) -> b.crtClient().minimumPartSizeInBytes(v));
    }

    @Override
    public Category<Long> defaultCategory(Class<?> paramType) {
        return new Unset();
    }

    public static final class FiveMiB implements Category<Long> {
        @Override
        public Long value() {
            return 5 * MIB;
        }
    }

    public static final class ThirtyTwoMiB implements Category<Long> {
        @Override
        public Long value() {
            return 32 * MIB;
        }
    }

    public static final class Unset implements Category<Long> {
        @Override
        public Long value() {
            return null;
        }
    }
}
