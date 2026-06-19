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

package software.amazon.awssdk.core.internal.crac;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;

/**
 * Discovers {@link SdkWarmUpProvider}s and invokes their warm-up behind the public {@code SdkWarmUp.prime()}.
 * Mirrors the {@code SdkHttpServiceProvider} loader abstraction, except warm-up invokes every discovered
 * provider rather than selecting one.
 */
@SdkInternalApi
public interface WarmUpInvoker {

    /**
     * Invokes {@link SdkWarmUpProvider#warmUp()} on every discovered provider, containing per-provider failures
     * so one failing provider does not stop the others.
     */
    void invokeAll();
}
