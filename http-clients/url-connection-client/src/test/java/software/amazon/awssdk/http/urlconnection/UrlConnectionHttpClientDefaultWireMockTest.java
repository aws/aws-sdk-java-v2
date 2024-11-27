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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.ProtocolException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientDefaultTestSuite;
import software.amazon.awssdk.http.SdkHttpMethod;

public class UrlConnectionHttpClientDefaultWireMockTest extends SdkHttpClientDefaultTestSuite {

    @Override
    protected SdkHttpClient createSdkHttpClient() {
        return UrlConnectionHttpClient.create();
    }

    @AfterEach
    public void reset() {
        HttpsURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
    }

    @Test
    @Override
    public void supportsRequestBodyOnGetRequest() throws Exception {
        // HttpURLConnection is hard-coded to switch GET requests with a body to POST requests, in #getOutputStream0.
        testForResponseCode(200, SdkHttpMethod.GET, SdkHttpMethod.POST, true);
    }

    @Test
    @Override
    public void supportsRequestBodyOnPatchRequest() {
        // HttpURLConnection does not support PATCH requests.
        assertThatThrownBy(super::supportsRequestBodyOnPatchRequest)
            .hasRootCauseInstanceOf(ProtocolException.class)
            .hasRootCauseMessage("Invalid HTTP method: PATCH");
    }
}
