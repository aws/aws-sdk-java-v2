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

package software.amazon.awssdk.services.s3.functionaltests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class EmptyResponseTest {
    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    @Test
    public void emptyChunkedEncodingResponseWorks() {
        stubFor(get(anyUrl())
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("Transfer-Encoding", "chunked")));

        S3Client client = S3Client.builder()
                                  .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                                  .region(Region.US_WEST_2)
                                  .credentialsProvider(AnonymousCredentialsProvider.create())
                                  .build();

        client.listBuckets(); // Should not fail
    }
}
