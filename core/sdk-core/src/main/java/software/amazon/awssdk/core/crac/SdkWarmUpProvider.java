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

package software.amazon.awssdk.core.crac;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Service Provider Interface for warming up an SDK service's request path before a Coordinated Restore at Checkpoint
 * (CRaC) checkpoint. The SDK discovers implementations on the classpath with {@link java.util.ServiceLoader}. An
 * implementation registers itself in the
 * {@code META-INF/services/software.amazon.awssdk.core.crac.SdkWarmUpProvider} resource.
 *
 * <p>
 * Implementations must be thread safe.
 */
@ThreadSafe
@SdkProtectedApi
public interface SdkWarmUpProvider {

    /**
     * Exercises the service's request path so the Just-In-Time compiled code is captured in a CRaC snapshot. This
     * method requires no credentials and performs no network I/O.
     */
    void warmUp();
}
