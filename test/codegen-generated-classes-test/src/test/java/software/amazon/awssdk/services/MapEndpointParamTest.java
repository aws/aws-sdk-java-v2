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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.endpoints.ProtocolRestJsonEndpointParams;
import software.amazon.awssdk.services.protocolrestjson.endpoints.ProtocolRestJsonEndpointProvider;

@WireMockTest
public class MapEndpointParamTest {

    private ProtocolRestJsonClient client;
    private ListOfStringsParamEndpointProvider endpointProvider;

    @BeforeEach
    public void init(WireMockRuntimeInfo wm) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create("key", "secret");

        endpointProvider = new ListOfStringsParamEndpointProvider(ProtocolRestJsonEndpointProvider.defaultProvider());

        client = ProtocolRestJsonClient.builder()
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                       .endpointProvider(endpointProvider)
                                       .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                       .build();

        stubFor(get(urlEqualTo("/"))
                    .willReturn(aResponse().withStatus(200)));
    }


    @Test
    void getOperationWithMapEndpointParam_requestWithCompleteListOfKeys_returnsRightValues() {
        Map<String, String> tableMetadataMap =
            IntStream.range(1, 4)
                     .boxed()
                     .collect(Collectors.toMap(num -> "table" + num, num -> "value" + num));

        client.getOperationWithMapEndpointParam(r -> r.mapOfStrings(tableMetadataMap));
        assertThat(endpointProvider.storedKeys().size()).isEqualTo(3);
        assertThat(endpointProvider.storedKeys().contains("table1")).isTrue();
        assertThat(endpointProvider.storedKeys().contains("table2")).isTrue();
        assertThat(endpointProvider.storedKeys().contains("table3")).isTrue();
    }

    private static class ListOfStringsParamEndpointProvider implements ProtocolRestJsonEndpointProvider {

        private List<String> storedKeys;
        ProtocolRestJsonEndpointProvider delegate;

        ListOfStringsParamEndpointProvider(ProtocolRestJsonEndpointProvider endpointProvider) {
            this.delegate = endpointProvider;
        }

        List<String> storedKeys() {
            return storedKeys;
        }

        @Override
        public CompletableFuture<Endpoint> resolveEndpoint(ProtocolRestJsonEndpointParams endpointParams) {
            List<String> keys = endpointParams.tables();
            if (keys != null) {
                storedKeys = keys;
            }
            return delegate.resolveEndpoint(endpointParams);
        }
    }
}
