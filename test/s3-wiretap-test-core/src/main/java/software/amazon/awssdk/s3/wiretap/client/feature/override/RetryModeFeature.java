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

import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.s3.wiretap.client.feature.AbstractFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.Category;

public final class RetryModeFeature extends AbstractFeature<RetryMode> {

    public RetryModeFeature() {
        super("retryMode", RetryMode.class);
        onSync((b, v) -> b.overrideConfig().retryStrategy(v));
        onAsync((b, v) -> b.overrideConfig().retryStrategy(v));
    }

    @Override
    public Category<RetryMode> defaultCategory(Class<?> paramType) {
        return new Unset();
    }

    public static final class Unset implements Category<RetryMode> {
        @Override
        public RetryMode value() {
            return null;
        }
    }

    public static final class Legacy implements Category<RetryMode> {
        @Override
        public RetryMode value() {
            return RetryMode.LEGACY;
        }
    }

    public static final class Standard implements Category<RetryMode> {
        @Override
        public RetryMode value() {
            return RetryMode.STANDARD;
        }
    }

    public static final class Adaptive implements Category<RetryMode> {
        @Override
        public RetryMode value() {
            return RetryMode.ADAPTIVE;
        }
    }

    public static final class AdaptiveV2 implements Category<RetryMode> {
        @Override
        public RetryMode value() {
            return RetryMode.ADAPTIVE_V2;
        }
    }
}
