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

package software.amazon.awssdk.internal.http;

import javax.net.ssl.KeyManager;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.TlsKeyManagersProvider;

/**
 * Simple implementation of {@link TlsKeyManagersProvider} that return a null array.
 * <p>
 * Use this provider if you don't want the client to present any certificates to the remote TLS host.
 */
@SdkInternalApi
public final class NoneTlsKeyManagersProvider implements TlsKeyManagersProvider {
    private static final NoneTlsKeyManagersProvider INSTANCE = new NoneTlsKeyManagersProvider();

    private NoneTlsKeyManagersProvider() {
    }

    @Override
    public KeyManager[] keyManagers() {
        return null;
    }

    public static NoneTlsKeyManagersProvider getInstance() {
        return INSTANCE;
    }
}
