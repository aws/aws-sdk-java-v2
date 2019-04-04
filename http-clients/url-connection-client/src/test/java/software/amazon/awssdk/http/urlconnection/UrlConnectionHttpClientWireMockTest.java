/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientTestSuite;
import software.amazon.awssdk.utils.AttributeMap;

public final class UrlConnectionHttpClientWireMockTest extends SdkHttpClientTestSuite {

    @Override
    protected SdkHttpClient createSdkHttpClient(SdkHttpClientOptions options) {
        return UrlConnectionHttpClient.create();
    }

    @After
    public void reset() {
        HttpsURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
    }

    @Test
    public void trustAllCertificates_shouldWork() throws Exception {
        try (SdkHttpClient client = UrlConnectionHttpClient.builder()
                                                           .buildWithDefaults(AttributeMap.builder()
                                                                                          .put(TRUST_ALL_CERTIFICATES,
                                                                                               Boolean.TRUE)
                                                                                          .build())) {
            testForResponseCodeUsingHttps(client, HttpURLConnection.HTTP_OK);
        }
    }
}
