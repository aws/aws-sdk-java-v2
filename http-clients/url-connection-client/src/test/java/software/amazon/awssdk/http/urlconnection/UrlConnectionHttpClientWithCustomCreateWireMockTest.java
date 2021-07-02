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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.FunctionalUtils.safeFunction;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.function.Function;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientTestSuite;

public final class UrlConnectionHttpClientWithCustomCreateWireMockTest extends SdkHttpClientTestSuite {

    private Function<HttpURLConnection, HttpURLConnection> connectionInterceptor = Function.identity();

    @Override
    protected SdkHttpClient createSdkHttpClient(SdkHttpClientOptions options) {
        return UrlConnectionHttpClient.create(uri -> invokeSafely(() -> {
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            return connectionInterceptor.apply(connection);
        }));
    }

    @Ignore // Not supported when using custom factory
    @Override
    public void testCustomTlsTrustManager() {
    }

    @Ignore // Not supported when using custom factory
    @Override
    public void testTrustAllWorks() {
    }

    @Ignore // Not supported when using custom factory
    @Override
    public void testCustomTlsTrustManagerAndTrustAllFails() {
    }

    @Test
    public void testGetResponseCodeNpeIsWrappedAsIo() throws Exception {
        connectionInterceptor = safeFunction(connection -> {
            connection = spy(connection);
            doThrow(new NullPointerException()).when(connection).getResponseCode();
            return connection;
        });

        assertThatThrownBy(() -> testForResponseCode(HttpURLConnection.HTTP_OK))
            .isInstanceOf(IOException.class)
            .hasMessage("Unexpected NullPointerException when trying to read response from HttpURLConnection")
            .hasCauseInstanceOf(NullPointerException.class);
    }
}
