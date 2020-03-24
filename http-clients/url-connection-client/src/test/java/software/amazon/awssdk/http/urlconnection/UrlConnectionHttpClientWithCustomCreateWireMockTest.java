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

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.HttpURLConnection;
import org.junit.Ignore;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientTestSuite;

public final class UrlConnectionHttpClientWithCustomCreateWireMockTest extends SdkHttpClientTestSuite {
    @Override
    protected SdkHttpClient createSdkHttpClient(SdkHttpClientOptions options) {
        return UrlConnectionHttpClient.create((uri) -> invokeSafely(() -> (HttpURLConnection) uri.toURL().openConnection()));
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
}
