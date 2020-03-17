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

import javax.net.ssl.KeyManager;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.internal.http.NoneTlsKeyManagersProvider;

/**
 * Provider for the {@link KeyManager key managers} to be used by the SDK when
 * creating the SSL context. Key managers are used when the client is required
 * to authenticate with the remote TLS peer, such as an HTTP proxy.
 */
@SdkPublicApi
@FunctionalInterface
public interface TlsKeyManagersProvider {

    /**
     * @return The {@link KeyManager}s, or {@code null}.
     */
    KeyManager[] keyManagers();

    /**
     * @return A provider that returns a {@code null} array of {@link KeyManager}s.
     */
    static TlsKeyManagersProvider noneProvider() {
        return NoneTlsKeyManagersProvider.getInstance();
    }
}
