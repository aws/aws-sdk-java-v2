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

package software.amazon.awssdk.services.kinesis;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.ProtocolNegotiation;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class AlpnH1CloseNotifyIntegTest extends AwsTestBase {

    private static KinesisAsyncClient asyncClientAlpn;

    @BeforeAll
    public static void init() {
        System.setProperty("javax.net.debug", "ssl,handshake");

        /*System.setProperty("jdk.tls.client.enableSessionTicketExtension", "false");
        System.setProperty("jdk.tls.client.enableSessionResumption", "false");*/

        asyncClientAlpn = KinesisAsyncClient.builder()
                                            .httpClient(NettyNioAsyncHttpClient.builder()
                                                                               .protocol(Protocol.HTTP1_1)
                                                                               .protocolNegotiation(ProtocolNegotiation.ALPN)
                                                                               .build())
                                            .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                            .build();
    }

    @AfterAll
    public static void cleanUp() {
        asyncClientAlpn.close();
    }

    @Test
    public void alpnH1_requestAfterReceivingCloseNotify_shouldSucceed() {
        // send requests until we get Rate Exceeded error, which will trigger close_notify
        boolean triggeredError = false;
        int count = 0;
        while (!triggeredError) {
            try {
                System.out.println();
                System.out.println("Sending request #" + ++count);
                System.out.println();
                asyncClientAlpn.describeLimits().join();
            } catch (Exception e) {
                triggeredError = true;
                System.out.println("Triggered error and close_notify $$$$$");
                System.out.println(e.getMessage());
                System.out.println();
            }
        }

        // sending ALPN H1 request with AFTER receiving close_notify should not hang
        System.out.println("Sending request AFTER receiving close notify");
        asyncClientAlpn.describeLimits().join();

        System.out.println("Success!");
    }

    @Test
    public void alpnH1_requestAfterReceivingCloseNotify_concurrent() {
        asyncClientAlpn.describeLimits();
        asyncClientAlpn.describeLimits();


    }
}
