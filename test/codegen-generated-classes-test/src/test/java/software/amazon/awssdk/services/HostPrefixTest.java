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

package software.amazon.awssdk.services;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.testutils.service.http.MockAyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockHttpClient;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.builder.SdkBuilder;

public class HostPrefixTest {

    private MockHttpClient mockHttpClient;
    private ProtocolRestJsonClient client;
    private MockAyncHttpClient mockAsyncClient;

    private ProtocolRestJsonAsyncClient asyncClient;

    @Before
    public void setupClient() {
        mockHttpClient = new MockHttpClient();
        mockAsyncClient = new MockAyncHttpClient();
        client = ProtocolRestJsonClient.builder()
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid",
                                                                                                                        "skid")))
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create("http://localhost"))
                                       .httpClient(mockHttpClient)
                                       .build();

        asyncClient = ProtocolRestJsonAsyncClient.builder()
                                                 .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                 .region(Region.US_EAST_1)
                                                 .endpointOverride(URI.create("http://localhost"))
                                                 .httpClient(mockAsyncClient)
                                                 .build();
    }

    @Test
    public void invalidHostPrefix_shouldThrowException() {
        assertThatThrownBy(() -> client.operationWithHostPrefix(b -> b.stringMember("123#")))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must only contain alphanumeric characters and "
                                                                               + "dashes");

        assertThatThrownBy(() -> asyncClient.operationWithHostPrefix(b -> b.stringMember("123#")).join()).hasCauseInstanceOf(IllegalArgumentException.class).hasMessageContaining("must only contain alphanumeric characters and dashes");
    }

    @Test
    public void nullHostPrefix_shouldThrowException() {
        assertThatThrownBy(() -> client.operationWithHostPrefix(SdkBuilder::build))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("component is missing");

        assertThatThrownBy(() -> asyncClient.operationWithHostPrefix(SdkBuilder::build).join())
            .hasCauseInstanceOf(IllegalArgumentException.class).hasMessageContaining("component is missing");
    }

    @Test
    public void syncValidHostPrefix_shouldPrefixEndpoint() {
       mockHttpClient.stubNextResponse(HttpExecuteResponse.builder()
                                                          .response(SdkHttpResponse.builder().statusCode(200)
                                                                                             .build())
                                                          .responseBody(AbortableInputStream.create(new StringInputStream("")))
                                                          .build());
        client.operationWithHostPrefix(b -> b.stringMember("123"));
        assertThat(mockHttpClient.getLastRequest().getUri().getHost()).isEqualTo("123-foo.localhost");

    }

    @Test
    public void asyncValidHostPrefix_shouldPrefixEndpoint() {
        mockAsyncClient.stubNextResponse(HttpExecuteResponse.builder()
                                                           .response(SdkHttpResponse.builder().statusCode(200)
                                                                                    .build())
                                                           .responseBody(AbortableInputStream.create(new StringInputStream("")))
                                                           .build());
        asyncClient.operationWithHostPrefix(b -> b.stringMember("123")).join();
        assertThat(mockAsyncClient.getLastRequest().getUri().getHost()).isEqualTo("123-foo.localhost");
    }
}
