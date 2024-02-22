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

package software.amazon.awssdk.http.apache;

import java.util.concurrent.ThreadLocalRandom;
import org.apache.http.conn.HttpHostConnectException;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.proxy.HttpClientDefaultPoxyConfigTest;
import java.net.ConnectException;

public class ApacheClientProxyConfigurationTest extends HttpClientDefaultPoxyConfigTest {

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
        throw new IllegalArgumentException("Async client is not supported");
    }

    @Override
    protected SdkHttpClient createSyncHttpClientWithDefaultProxy() {
        return ApacheHttpClient.builder().build();
    }

    /**
     * Wrire code which inputs an integer args and returns one number between 0 to 65535 excluding number args
     */
    private int getRandomPort(int currentPort) {
        int randomPort;
        do {
            randomPort = ThreadLocalRandom.current().nextInt(65535);
        } while (randomPort == currentPort);
        return randomPort;
    }


}