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

public final class TargetThroughputFeature extends AbstractFeature<Double> {

    public TargetThroughputFeature() {
        super("targetThroughput", Double.class);
        onCrt((b, v) -> b.crtClient().targetThroughputInGbps(v));
    }

    @Override
    public Category<Double> defaultCategory(Class<?> paramType) {
        return new Unset();
    }

    public static final class High implements Category<Double> {
        @Override
        public Double value() {
            return 100.0;
        }
    }

    public static final class Gbps15 implements Category<Double> {
        @Override
        public Double value() {
            return 15.0;
        }
    }

    public static final class Unset implements Category<Double> {
        @Override
        public Double value() {
            return null;
        }
    }
}
