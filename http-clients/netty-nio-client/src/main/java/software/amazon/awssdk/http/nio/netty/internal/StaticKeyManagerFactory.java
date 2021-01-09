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

package software.amazon.awssdk.http.nio.netty.internal;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Factory that simply returns a statically provided set of {@link KeyManager}s.
 */
@SdkInternalApi
public final class StaticKeyManagerFactory extends KeyManagerFactory {
    private StaticKeyManagerFactory(KeyManager[] keyManagers) {
        super(new StaticKeyManagerFactorySpi(keyManagers), null, null);
    }

    public static StaticKeyManagerFactory create(KeyManager[] keyManagers) {
        return new StaticKeyManagerFactory(keyManagers);
    }
}
