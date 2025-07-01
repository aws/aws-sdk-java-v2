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

import java.net.InetAddress;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientLocalAddressFunctionalTestSuite;

@DisplayName("Apache5 HTTP Client - Local Address Functional Tests")
class Apache5HttpClientLocalAddressOnBuilderFunctionalTest extends SdkHttpClientLocalAddressFunctionalTestSuite {

    @Override
    protected SdkHttpClient createHttpClient(InetAddress localAddress, Duration connectionTimeout) {

        return Apache5HttpClient.builder()
                                .localAddress(localAddress)
                                .connectionTimeout(connectionTimeout)
                                .build();
    }
}
