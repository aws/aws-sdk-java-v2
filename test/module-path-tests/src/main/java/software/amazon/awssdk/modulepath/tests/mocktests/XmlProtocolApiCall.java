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

package software.amazon.awssdk.modulepath.tests.mocktests;

import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlAsyncClient;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlClient;

/**
 * Protocol tests for xml protocol
 */
public class XmlProtocolApiCall extends BaseMockApiCall {

    private ProtocolRestXmlClient client;
    private ProtocolRestXmlAsyncClient asyncClient;

    public XmlProtocolApiCall() {
        super("xml");
        client = ProtocolRestXmlClient.builder().httpClient(mockHttpClient).build();
        asyncClient = ProtocolRestXmlAsyncClient.builder().httpClient(mockAyncHttpClient).build();
    }

    @Override
    Runnable runnable() {
        return () -> client.allTypes();
    }

    @Override
    Runnable asyncRunnable() {
        return () -> asyncClient.allTypes().join();
    }
}
