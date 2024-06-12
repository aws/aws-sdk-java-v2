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

package software.amazon.awssdk.retries.api.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * Implementation of the {@link AcquireInitialTokenRequest} interface.
 */
@SdkInternalApi
public final class AcquireInitialTokenRequestImpl implements AcquireInitialTokenRequest {

    private final String scope;

    private AcquireInitialTokenRequestImpl(String scope) {
        this.scope = Validate.paramNotNull(scope, "scope");
    }

    @Override
    public String scope() {
        return scope;
    }

    /**
     * Creates a new {@link AcquireInitialTokenRequestImpl} instance with the given scope.
     */
    public static AcquireInitialTokenRequest create(String scope) {
        return new AcquireInitialTokenRequestImpl(scope);
    }
}
