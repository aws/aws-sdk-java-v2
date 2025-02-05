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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;


import java.time.Duration;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientTestSuite;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Testing the scenario where h1 server sends 5xx errors.
 */
public class AwsCrtHttpClientTest extends SdkHttpClientTestSuite {

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("aws.crt.debugnative", "true");
        Log.initLoggingToStdout(Log.LogLevel.Warn);
    }

    @AfterAll
    public static void afterAll() {
        CrtResource.waitForNoResources();
    }

    /**
     * default value of connectionAcquisitionTimeout of 10 will fail validatesHttpsCertificateIssuer() test
     * */
    @Override
    protected SdkHttpClient createSdkHttpClient(SdkHttpClientOptions options) {
        boolean trustAllCerts = options.trustAll();
        return AwsCrtHttpClient.builder()
                               .connectionAcquisitionTimeout(Duration.ofSeconds(40))
                               .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, trustAllCerts).build());
    }

    @Test
    public void negativeConnectionAcquisitionTimeout_shouldFail() {
        assertThatThrownBy(() -> {
            SdkHttpClient client = AwsCrtHttpClient.builder()
                    .connectionAcquisitionTimeout(Duration.ofSeconds(-1))
                    .build();
            client.close();
        }).hasMessage("connectionAcquisitionTimeout must be positive");
    }

    // Empty test; behavior not supported when using custom factory
    @Override
    public void testCustomTlsTrustManagerAndTrustAllFails() {
    }

    // Empty test; behavior not supported when using custom factory
    @Override
    public void testCustomTlsTrustManager() {
    }
}
