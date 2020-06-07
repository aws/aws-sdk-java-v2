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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.crt.io.HostResolver;
import software.amazon.awssdk.http.H1ServerBehaviorTestBase;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

/**
 * Testing the scenario where h1 server sends 5xx errors.
 */
public class H1ServerBehaviorTest extends H1ServerBehaviorTestBase {
    private SdkAsyncHttpClient crtClient;

    @Override
    protected SdkAsyncHttpClient getTestClient() { return crtClient; }

    @Before
    public void setup() throws Exception {
        super.setup();

        int numThreads = Runtime.getRuntime().availableProcessors();
        try (EventLoopGroup eventLoopGroup = new EventLoopGroup(numThreads);
             HostResolver hostResolver = new HostResolver(eventLoopGroup)) {

            crtClient = AwsCrtAsyncHttpClient.builder()
                    .eventLoopGroup(eventLoopGroup)
                    .hostResolver(hostResolver)
                    .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build());
        }
    }


    @After
    public void teardown() throws InterruptedException {
        super.teardown();

        if (crtClient != null) {
            crtClient.close();
        }
        crtClient = null;
    }

    @Test
    public void connectionReceive500_shouldNotReuseConnection() {
        assertThat(crtClient).isNotNull();
        super.connectionReceive500_shouldNotReuseConnection();
    }

    @Test
    public void connectionReceive200_shouldReuseConnection() {
        assertThat(crtClient).isNotNull();
        super.connectionReceive200_shouldReuseConnection();
    }

    @Test
    public void connectionCloseHeader_shouldNotReuseConnection() throws InterruptedException {
        assertThat(crtClient).isNotNull();
        super.connectionCloseHeader_shouldNotReuseConnection();
    }
}
