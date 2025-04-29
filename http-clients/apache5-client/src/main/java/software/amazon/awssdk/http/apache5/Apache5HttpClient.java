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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;

/**
 * An implementation of {@link SdkHttpClient} that uses Apache HTTP Client 5.x to communicate with the service. This client
 * provides enhanced functionality over the URL connection client, including support for HTTP proxies, connection pooling,
 * and advanced configuration options.
 *
 * <p>This implementation leverages Apache HttpClient 5.x, offering improved performance characteristics and better compliance
 * with HTTP standards compared to the Apache 4.x-based.</p>
 *
 * <p>See software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient for a lighter alternative implementation
 * with fewer dependencies but more limited functionality.</p>
 *
 */
@SdkPublicApi
public class Apache5HttpClient implements SdkHttpClient {

    public static final String CLIENT_NAME = "Apache5";

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        throw new UnsupportedOperationException("API implementation is in progress");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("API implementation is in progress");
    }

    @Override
    public String clientName() {
        return CLIENT_NAME;
    }

}
