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

import io.netty.handler.ssl.util.SimpleTrustManagerFactory;
import java.security.KeyStore;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class StaticTrustManagerFactory extends SimpleTrustManagerFactory {
    private final TrustManager[] trustManagers;

    private StaticTrustManagerFactory(TrustManager[] trustManagers) {
        this.trustManagers = trustManagers;
    }

    @Override
    protected void engineInit(KeyStore keyStore) {
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) {
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return trustManagers;
    }

    public static TrustManagerFactory create(TrustManager[] trustManagers) {
        return new StaticTrustManagerFactory(trustManagers);
    }
}
