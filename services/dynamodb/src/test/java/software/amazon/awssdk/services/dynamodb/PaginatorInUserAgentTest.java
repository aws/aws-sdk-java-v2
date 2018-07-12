/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.paginators.QueryPublisher;

public class PaginatorInUserAgentTest {

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    private DynamoDbClient dynamoDbClient;
    private DynamoDbAsyncClient dynamoDbAsyncClient;

    @Before
    public void setup() {
        dynamoDbClient = DynamoDbClient.builder()
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test",
                                                                                                                        "test")))
                                       .region(Region.US_WEST_2).endpointOverride(URI.create("http://localhost:" + mockServer
                .port()))
                                       .build();

        dynamoDbAsyncClient = DynamoDbAsyncClient.builder()
                                                 .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
                                                                                                           .create("test",
                                                                                                                                  "test")))
                                                 .region(Region.US_WEST_2).endpointOverride(URI.create("http://localhost:" +
                                                                                                       mockServer.port()))
                                                 .build();
    }


    @Test
    public void syncPaginator_shouldHavePaginatorUserAgent() throws IOException {
        stubFor(any(urlEqualTo("/"))
                    .willReturn(aResponse()
                                    .withStatus(500)));
        try {
            dynamoDbClient.queryPaginator(b -> b.tableName("test")).items().iterator().next();
        } catch (Exception e) {
            //expected
        }

        verify(postRequestedFor(urlEqualTo("/")).withHeader("User-Agent", containing("PAGINATED/" + VersionInfo.SDK_VERSION)));
    }

    @Test
    public void asyncPaginator_shouldHavePaginatorUserAgent() throws IOException {
        stubFor(any(urlEqualTo("/"))
                    .willReturn(aResponse()
                                    .withStatus(500)));
        try {
            QueryPublisher queryPublisher = dynamoDbAsyncClient.queryPaginator(b -> b.tableName("test"));
            queryPublisher.items().forEach(a -> a.get("")).get();
        } catch (Exception e) {
            //expected
        }

        verify(postRequestedFor(urlEqualTo("/")).withHeader("User-Agent", containing("PAGINATED/" + VersionInfo.SDK_VERSION)));
    }

}
