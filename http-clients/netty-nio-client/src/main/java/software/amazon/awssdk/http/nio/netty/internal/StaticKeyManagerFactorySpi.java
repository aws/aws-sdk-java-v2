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

import java.security.KeyStore;
import java.util.Arrays;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;


/**
* Factory SPI that simply returns a statically provided set of {@link KeyManager}s.
*/
@SdkInternalApi
public final class StaticKeyManagerFactorySpi extends KeyManagerFactorySpi {
    private final KeyManager[] keyManagers;

    public StaticKeyManagerFactorySpi(KeyManager[] keyManagers) {
        Validate.paramNotNull(keyManagers, "keyManagers");
        this.keyManagers = Arrays.copyOf(keyManagers, keyManagers.length);
    }

    @Override
    protected void engineInit(KeyStore ks, char[] password) {
        throw new UnsupportedOperationException("engineInit not supported by this KeyManagerFactory");
    }

    @Override
    protected void engineInit(ManagerFactoryParameters spec) {
        throw new UnsupportedOperationException("engineInit not supported by this KeyManagerFactory");
    }

    @Override
    protected KeyManager[] engineGetKeyManagers() {
        return keyManagers;
    }
}
