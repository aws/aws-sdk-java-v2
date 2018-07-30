/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.apache;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import java.net.HttpURLConnection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientTestSuite;
import software.amazon.awssdk.utils.AttributeMap;

@RunWith(MockitoJUnitRunner.class)
public class ApacheHttpClientWireMockTest extends SdkHttpClientTestSuite {
    @Override
    protected SdkHttpClient createSdkHttpClient(SdkHttpClientOptions options) {
        return ApacheHttpClient.builder().build();
    }

    @Test
    public void noSslException_WhenCertCheckingDisabled() throws Exception {
        SdkHttpClient client = ApacheHttpClient.builder()
                                               .buildWithDefaults(AttributeMap.builder()
                                                                              .put(TRUST_ALL_CERTIFICATES, Boolean.TRUE)
                                                                              .build());

        testForResponseCodeUsingHttps(client, HttpURLConnection.HTTP_OK);
    }
}
