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

import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.s3.wiretap.client.feature.AbstractFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.Category;
import software.amazon.awssdk.services.s3.crt.S3CrtRetryConfiguration;

public final class RetryFeature extends AbstractFeature<Boolean> {

    public RetryFeature() {
        super("retry", Boolean.class);
        onSync((b, retriesOn) -> {
            if (retriesOn) {
                return;
            }
            b.overrideConfig().retryPolicy(RetryPolicy.none());
        });
        onAsync((b, retriesOn) -> {
            if (retriesOn) {
                return;
            }
            b.overrideConfig().retryPolicy(RetryPolicy.none());
        });
        onCrt((b, retriesOn) -> {
            if (retriesOn) {
                return;
            }
            b.crtClient().retryConfiguration(S3CrtRetryConfiguration.builder().numRetries(0).build());
        });
    }

    @Override
    public Category<Boolean> defaultCategory(Class<?> paramType) {
        return new Default();
    }

    public static final class Default implements Category<Boolean> {
        @Override
        public Boolean value() {
            return true;
        }
    }

    public static final class None implements Category<Boolean> {
        @Override
        public Boolean value() {
            return false;
        }
    }
}
