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
package software.amazon.awssdk.http.urlconnection;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.junit.After;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientTestSuite;
import software.amazon.awssdk.utils.AttributeMap;

public final class UrlConnectionHttpClientWireMockTest extends SdkHttpClientTestSuite {

    @Override
    protected SdkHttpClient createSdkHttpClient(SdkHttpClientOptions options) {
        UrlConnectionHttpClient.Builder builder = UrlConnectionHttpClient.builder();
        AttributeMap.Builder attributeMap = AttributeMap.builder();

        if (options.tlsTrustManagersProvider() != null) {
            builder.tlsTrustManagersProvider(options.tlsTrustManagersProvider());
        }

        if (options.trustAll()) {
            attributeMap.put(TRUST_ALL_CERTIFICATES, options.trustAll());
        }

        return builder.buildWithDefaults(attributeMap.build());
    }

    @After
    public void reset() {
        HttpsURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
    }
}
