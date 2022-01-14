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

package software.amazon.awssdk.services.s3;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;

public class EndpointOverrideTest {

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    private S3Client s3Client;

    private S3AsyncClient s3AsyncClient;

    @Before
    public void setup() {
        s3Client = S3Client.builder()
                           .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                           .region(Region.US_WEST_2).endpointOverride(URI.create(getEndpoint()))
                           .build();

        s3AsyncClient = S3AsyncClient.builder()
                                     .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                     .region(Region.US_WEST_2)
                                     .endpointOverride(URI.create(getEndpoint()))
                                     .build();
    }

    private String getEndpoint() {
        return "http://localhost:" + mockServer.port();
    }

    //https://github.com/aws/aws-sdk-java-v2/issues/437
    @Test
    public void getObjectAsync_shouldNotThrowNPE() throws IOException {
        stubFor(get(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("<?xml version=\"1.0\"?><GetObjectResult xmlns=\"http://s3"
                                              + ".amazonaws.com/doc/2006-03-01\"></GetObjectResult>")));
        assertThat(s3AsyncClient.getObject(b -> b.bucket("test").key("test").build(),
                                           AsyncResponseTransformer.toBytes()).join()).isNotNull();
    }

    @Test
    public void getObject_shouldNotThrowNPE() {
        stubFor(get(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(200).withBody("<?xml version=\"1.0\"?><GetObjectResult xmlns=\"http://s3"
                                                              + ".amazonaws.com/doc/2006-03-01\"></GetObjectResult>")));
        assertThat(s3Client.listObjectVersions(b -> b.bucket("test"))).isNotNull();
    }
}
