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

package software.amazon.awssdk.services.dynamodb;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.net.URI;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;
import software.amazon.awssdk.services.dynamodb.paginators.ScanPublisher;

public class PaginatorTest {
    private static final WireMockServer wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    private static final ObjectMapper mapper = new ObjectMapper();
    private static DynamoDbAsyncClient ddbAsync;
    private static DynamoDbClient ddb;

    @BeforeAll
    static void setup() {
        wireMock.start();

        ddbAsync = DynamoDbAsyncClient.builder()
                                      .region(Region.US_WEST_2)
                                      .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                      .credentialsProvider(AnonymousCredentialsProvider.create())
                                      .build();

        ddb = DynamoDbClient.builder()
                            .region(Region.US_WEST_2)
                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                            .credentialsProvider(AnonymousCredentialsProvider.create())
                            .build();
    }

    @AfterAll
    static void teardown() {
        ddb.close();
        ddbAsync.close();
        wireMock.stop();
    }

    @Test
    void scanPaginator_async_largePage_subscribe_succeeds() {
        int nItems = 10_000;
        wireMock.stubFor(WireMock.any(anyUrl())
                                 .willReturn(aResponse()
                                                 .withStatus(200)
                                                 .withJsonBody(createScanResponse(nItems))));

        ScanPublisher publisher = ddbAsync.scanPaginator(ScanRequest.builder().build());

        AtomicLong counter = new AtomicLong();
        publisher.items().subscribe(item -> counter.incrementAndGet()).join();
        assertThat(counter.get()).isEqualTo(nItems);
    }

    @Test
    void scanPaginator_sync_largePage_subscribe_succeeds() {
        int nItems = 10_000;
        wireMock.stubFor(WireMock.any(anyUrl())
                                 .willReturn(aResponse()
                                                 .withStatus(200)
                                                 .withJsonBody(createScanResponse(nItems))));

        ScanIterable iterable = ddb.scanPaginator(ScanRequest.builder().build());

        AtomicLong counter = new AtomicLong();
        iterable.items().forEach(item -> counter.incrementAndGet());
        assertThat(counter.get()).isEqualTo(nItems);
    }

    private static JsonNode createScanResponse(int nItems) {
        ObjectNode resp = mapper.createObjectNode();
        resp.set("Count", mapper.valueToTree(nItems));

        ArrayNode items = mapper.createArrayNode();

        for (int i = 0; i < nItems; i++) {
            // {
            //   "id": {
            //     "N": 1
            //   }
            // }
            ObjectNode item = mapper.createObjectNode();
            ObjectNode idNode = mapper.createObjectNode();
            idNode.put("N", mapper.valueToTree(i));
            item.set("id", idNode);
            items.add(item);
        }

        resp.set("Items", items);

        return resp;
    }
}
