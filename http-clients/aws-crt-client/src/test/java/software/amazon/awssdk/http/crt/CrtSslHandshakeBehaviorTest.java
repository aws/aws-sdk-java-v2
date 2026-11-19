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

package software.amazon.awssdk.http.crt;

import java.time.Duration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientSslHandshakeBehaviorTestSuite;

public class CrtSslHandshakeBehaviorTest extends SdkHttpClientSslHandshakeBehaviorTestSuite {
    @Override
    protected SdkHttpClient createSdkHttpClient() {
        // CRT negotiates TLS during connection acquisition; the default 10s acquisition timeout can
        // expire first and surface an acquisition-timeout exception instead of SSLHandshakeException.
        return AwsCrtHttpClient.builder()
                               .connectionAcquisitionTimeout(Duration.ofSeconds(30))
                               .build();
    }
}
