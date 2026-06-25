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

package software.amazon.awssdk.core.internal.http.loader;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Warms the HTTP clients of one transport kind (sync or async) on the classpath for CRaC priming. {@code SdkWarmUp.prime()}
 * invokes the sync and async implementations uniformly as peers.
 */
@SdkInternalApi
public interface HttpClientWarmer {

    /**
     * Discovers and warms every HTTP client of this transport kind on the classpath. Best-effort; never throws.
     */
    void warmAll();
}
