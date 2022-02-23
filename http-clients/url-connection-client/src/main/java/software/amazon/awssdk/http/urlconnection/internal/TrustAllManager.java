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

package software.amazon.awssdk.http.urlconnection.internal;

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

/**
 * Insecure trust manager to trust all certs. Should only be used for testing.
 */
@SdkInternalApi
public class TrustAllManager implements X509TrustManager {
    public static final TrustAllManager INSTANCE = new TrustAllManager();

    private static final Logger log = UrlConnectionLogger.LOG;

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
        log.debug(() -> "Accepting a client certificate: " + x509Certificates[0].getSubjectDN());
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
        log.debug(() -> "Accepting a server certificate: " + x509Certificates[0].getSubjectDN());
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
