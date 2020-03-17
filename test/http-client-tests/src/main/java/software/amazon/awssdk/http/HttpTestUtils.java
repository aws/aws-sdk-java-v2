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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;

public class HttpTestUtils {
    private HttpTestUtils() {
    }

    public static WireMockServer createSelfSignedServer() {
        URL selfSignedJks = SdkHttpClientTestSuite.class.getResource("/selfSigned.jks");

        return new WireMockServer(wireMockConfig()
                                      .dynamicHttpsPort()
                                      .keystorePath(selfSignedJks.toString())
                                      .keystorePassword("changeit")
                                      .keystoreType("jks")
        );
    }

    public static KeyStore getSelfSignedKeyStore() throws Exception {
        URL selfSignedJks = SdkHttpClientTestSuite.class.getResource("/selfSigned.jks");
        KeyStore keyStore = KeyStore.getInstance("jks");
        try (InputStream stream = selfSignedJks.openStream()) {
            keyStore.load(stream, "changeit".toCharArray());
        }

        return keyStore;
    }
}
