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

package software.amazon.awssdk.s3.wiretap.client.feature.override;

import software.amazon.awssdk.s3.wiretap.client.feature.AbstractFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.Category;

public final class AppIdFeature extends AbstractFeature<String> {

    public AppIdFeature() {
        super("appId", String.class);
        onSync((b, v) -> b.overrideConfig().appId(v));
        onAsync((b, v) -> b.overrideConfig().appId(v));
    }

    @Override
    public Category<String> defaultCategory(Class<?> paramType) {
        return new Unset();
    }

    public static final class Unset implements Category<String> {
        @Override
        public String value() {
            return null;
        }
    }

    public static final class Set implements Category<String> {
        @Override
        public String value() {
            return "wiretap-test";
        }
    }
}
