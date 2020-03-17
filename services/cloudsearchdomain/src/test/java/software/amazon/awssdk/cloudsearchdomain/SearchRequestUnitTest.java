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
package software.amazon.awssdk.cloudsearchdomain;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudsearchdomain.CloudSearchDomainClient;
import software.amazon.awssdk.services.cloudsearchdomain.model.SearchRequest;

/**
 * Unit tests for {@link SearchRequest}.
 */
public class SearchRequestUnitTest {
    private static final AwsBasicCredentials CREDENTIALS = AwsBasicCredentials.create("access", "secret");

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(new WireMockConfiguration().port(0).notifier(new ConsoleNotifier(true)));

    private CloudSearchDomainClient searchClient;

    @Before
    public void testSetup() {
        searchClient = CloudSearchDomainClient.builder()
                                              .credentialsProvider(StaticCredentialsProvider.create(CREDENTIALS))
                                              .region(Region.US_EAST_1)
                                              .endpointOverride(URI.create("http://localhost:" + wireMockRule.port()))
                                              .build();
    }

    /**
     * Test that search requests use POST instead of (the also supported) GET.
     * @throws IOException
     */
    @Test
    public void testPostUsedForSearchRequest() throws IOException {
        stubFor(post(urlMatching("/.*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("{\"status\":{\"rid\":\"fooBar\",\"time-ms\":7},\"hits\":{\"found\":0,\"start\":0,\"hit\":[]}}")));

        searchClient.search(SearchRequest.builder().query("Lord of the Rings").build());

        verify(postRequestedFor(urlEqualTo("/2013-01-01/search"))
                   .withRequestBody(equalTo("format=sdk&pretty=true&q=Lord+of+the+Rings"))
                   .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded")));
    }
}
