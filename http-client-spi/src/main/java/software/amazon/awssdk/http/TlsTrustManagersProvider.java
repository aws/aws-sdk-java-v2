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

package software.amazon.awssdk.http;

import javax.net.ssl.TrustManager;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Provider for the {@link TrustManager trust managers} to be used by the SDK when
 * creating the SSL context. Trust managers are used when the client is checking
 * if the remote host can be trusted.
 */
@SdkPublicApi
@FunctionalInterface
public interface TlsTrustManagersProvider {
    /**
     * @return The {@link TrustManager}s, or {@code null}.
     */
    TrustManager[] trustManagers();
}
