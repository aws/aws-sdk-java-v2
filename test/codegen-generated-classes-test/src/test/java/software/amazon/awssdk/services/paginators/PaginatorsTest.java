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

package software.amazon.awssdk.services.paginators;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.reactivex.Flowable;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.paginators.model.ListStringsRequest;
import software.amazon.awssdk.services.paginators.paginators.ListStringsPublisher;

public class PaginatorsTest {
    private static final WireMockServer wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    private static final ObjectMapper mapper = new ObjectMapper();

    private static PaginatorsAsyncClient client;

    @BeforeAll
    static void setup() {
        wireMock.start();

        client = PaginatorsAsyncClient.builder()
                                      .region(Region.US_WEST_2)
                                      .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                      .credentialsProvider(AnonymousCredentialsProvider.create())
                                      .build();
    }

    @AfterAll
    static void teardown() {
        client.close();
        wireMock.stop();
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void listStrings_largePage_succeeds() {
        int nItems = 10_000;
        wireMock.stubFor(post(urlEqualTo("/2016-03-11/listStrings"))
                             .willReturn(aResponse().withStatus(200)
                                                    .withJsonBody(createScanResponse(nItems))));

        ListStringsPublisher publisher = client.listStringsPaginator(ListStringsRequest.builder().build());

        Long itemsSeen = Flowable.fromPublisher(publisher.strings()).count().blockingGet();
        assertThat(itemsSeen).isEqualTo(nItems);
    }

    private static JsonNode createScanResponse(int nItems) {
        ObjectNode resp = mapper.createObjectNode();

        ArrayNode strings = mapper.createArrayNode();
        for (int i = 0; i < nItems; i++) {
            strings.add(mapper.valueToTree(Integer.toString(i)));
        }

        resp.set("Strings", strings);

        return resp;
    }
}
