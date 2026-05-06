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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

public class Apache5NioAsyncHttpClientTest {

    @Test
    public void builderCreatesClient() {
        try (SdkAsyncHttpClient client = Apache5NioAsyncHttpClient.builder().build()) {
            assertThat(client).isNotNull();
            assertThat(client.clientName()).isEqualTo("Apache5Nio");
        }
    }

    @Test
    public void createFactoryMethod() {
        try (Apache5NioAsyncHttpClient client = Apache5NioAsyncHttpClient.create()) {
            assertThat(client.clientName()).isEqualTo("Apache5Nio");
        }
    }

    @Test
    public void builderWithConfigOptions() {
        try (SdkAsyncHttpClient client = Apache5NioAsyncHttpClient.builder()
                                                                   .socketTimeout(Duration.ofSeconds(30))
                                                                   .connectionTimeout(Duration.ofSeconds(10))
                                                                   .maxConnections(50)
                                                                   .build()) {
            assertThat(client).isNotNull();
        }
    }

    @Test
    public void spiServiceRegistered() {
        java.util.ServiceLoader<software.amazon.awssdk.http.async.SdkAsyncHttpService> loader =
            java.util.ServiceLoader.load(software.amazon.awssdk.http.async.SdkAsyncHttpService.class);
        boolean found = false;
        for (software.amazon.awssdk.http.async.SdkAsyncHttpService service : loader) {
            if (service instanceof Apache5NioAsyncHttpService) {
                found = true;
                break;
            }
        }
        assertThat(found).as("Apache5NioAsyncHttpService should be registered via SPI").isTrue();
    }
}
