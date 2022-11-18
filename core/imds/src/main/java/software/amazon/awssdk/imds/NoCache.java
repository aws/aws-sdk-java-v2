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

package software.amazon.awssdk.imds;

import java.time.Duration;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * No caching, a token request will be performed for every new metadata requests. This is the default behavior.
 */
@SdkPublicApi
public final class NoCache implements TokenCacheStrategy {
    @Override
    public <T> Supplier<T> getCachedSupplier(Supplier<T> valueSupplier, Duration staleTime) {
        return valueSupplier;
    }
}
