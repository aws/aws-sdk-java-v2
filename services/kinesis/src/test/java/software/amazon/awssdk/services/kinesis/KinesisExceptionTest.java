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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.InvalidArgumentException;
import software.amazon.awssdk.services.kinesis.model.ResourceNotFoundException;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class KinesisExceptionTest {
    private static final Logger logger = LoggerFactory.getLogger(KinesisExceptionTest.class);
    private WireMockServer wireMock;
    private KinesisClient client;

    @Before
    public void setup() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();

        client = KinesisClient.builder()
                              .endpointOverride(URI.create(wireMock.baseUrl()))
                              .region(Region.US_WEST_2)
                              .credentialsProvider(StaticCredentialsProvider.create(
                                  AwsBasicCredentials.create("fake-key", "fake-secret")))
                              .build();
    }

    @After
    public void tearDown() {
        wireMock.stop();
    }

    @Test
    public void testInvalidArgumentException() {
        wireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/"))
                                 .willReturn(WireMock.aResponse()
                                                     .withStatus(400)
                                                     .withHeader("x-amzn-ErrorType", "InvalidArgumentException")
                                                     .withHeader("Content-Type", "application/json")));

        GetRecordsRequest request = GetRecordsRequest.builder()
                                                     .shardIterator("Invalid-Shard-Iterator")
                                                     .build();

        assertThatThrownBy(() -> client.getRecords(request))
            .isInstanceOf(InvalidArgumentException.class)
            .satisfies(e -> {
                InvalidArgumentException exception = (InvalidArgumentException) e;
                assertThat(exception.statusCode()).isEqualTo(400);
                assertThat(exception.awsErrorDetails().errorCode()).isEqualTo("InvalidArgumentException");
            });
    }


    @Test
    public void testResourceNotFoundException() {
        wireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/"))
                                 .willReturn(WireMock.aResponse()
                                                     .withStatus(400)
                                                     .withHeader("x-amzn-ErrorType", "ResourceNotFoundException")));

        GetRecordsRequest request = GetRecordsRequest.builder()
                                                     .shardIterator("NonExistent-Shard-Iterator")
                                                     .build();

        assertThatThrownBy(() -> client.getRecords(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .satisfies(e -> {
                ResourceNotFoundException exception = (ResourceNotFoundException) e;
                assertThat(exception.statusCode()).isEqualTo(400);
                assertThat(exception.awsErrorDetails().errorCode()).isEqualTo("ResourceNotFoundException");
            });
    }
}
