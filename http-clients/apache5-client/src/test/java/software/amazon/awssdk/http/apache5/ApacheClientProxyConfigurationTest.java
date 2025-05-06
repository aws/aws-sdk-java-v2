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

package software.amazon.awssdk.http.apache5;

import java.net.ConnectException;
import org.apache.hc.client5.http.HttpHostConnectException;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.proxy.HttpClientDefaultProxyConfigTestSuite;

public class ApacheClientProxyConfigurationTest extends HttpClientDefaultProxyConfigTestSuite {

    @Override
    protected Class<? extends Exception> getProxyFailedExceptionType() {
        return HttpHostConnectException.class;

    }

    @Override
    protected Class<? extends Exception> getProxyFailedCauseExceptionType() {
        return ConnectException.class;
    }

    @Override
    protected boolean isSyncClient() {
        return true;
    }

    @Override
    protected SdkAsyncHttpClient createHttpClientWithDefaultProxy() {
        throw new IllegalArgumentException("Async client  is not supported for this test.");
    }

    @Override
    protected SdkHttpClient createSyncHttpClientWithDefaultProxy() {
        return ApacheHttpClient.create();
    }

}