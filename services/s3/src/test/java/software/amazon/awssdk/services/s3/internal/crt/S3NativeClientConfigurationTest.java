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

package software.amazon.awssdk.services.s3.internal.crt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.http.HttpProxyOptions;
import software.amazon.awssdk.services.s3.crt.S3CrtHttpConfiguration;
import software.amazon.awssdk.services.s3.crt.S3CrtProxyConfiguration;
import software.amazon.awssdk.utils.internal.SystemSettingUtilsTestBackdoor;

class S3NativeClientConfigurationTest {
    @BeforeEach
    public void preCleanup() {
        SystemSettingUtilsTestBackdoor.clearEnvironmentVariableOverrides();
    }

    @BeforeEach
    public void postCleanup() {
        SystemSettingUtilsTestBackdoor.clearEnvironmentVariableOverrides();
    }

    @Test
    public void testAllowsSeparatingProxies() {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "http_proxy",
            "https://user:pass@example.com:1234"
        );
        S3NativeClientConfiguration configuration = S3NativeClientConfiguration.builder().httpConfiguration(
            S3CrtHttpConfiguration.builder()
                                  .proxyConfiguration(
                                      S3CrtProxyConfiguration.builder()
                                                             .host("localhost")
                                                             .port(4321)
                                                             .scheme("http")
                                                             .username("other")
                                                             .password("other-pass")
                                                             .proxyOverHttps(false)
                                                             .build()
                                  )
                                  .build()
        ).build();

        HttpProxyOptions httpProxy = configuration.httpProxyOptions();
        assertThat(httpProxy.getHost()).isEqualTo("localhost");
        assertThat(httpProxy.getPort()).isEqualTo(4321);
        assertThat(httpProxy.getAuthorizationUsername()).isEqualTo("other");
        assertThat(httpProxy.getAuthorizationPassword()).isEqualTo("other-pass");

        HttpProxyOptions httpsProxy = configuration.httpsProxyOptions();
        assertThat(httpsProxy.getHost()).isEqualTo("example.com");
        assertThat(httpsProxy.getPort()).isEqualTo(1234);
        assertThat(httpsProxy.getAuthorizationUsername()).isEqualTo("user");
        assertThat(httpsProxy.getAuthorizationPassword()).isEqualTo("pass");
    }
}
