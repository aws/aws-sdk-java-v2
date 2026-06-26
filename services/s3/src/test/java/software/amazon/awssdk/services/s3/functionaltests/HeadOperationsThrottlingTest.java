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
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class HeadOperationsThrottlingTest {

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    private S3Client client;

    @Before
    public void setup() {
        client = S3Client.builder()
                         .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                         .credentialsProvider(() -> AwsBasicCredentials.create("test", "test"))
                         .forcePathStyle(true)
                         .region(Region.US_EAST_1)
                         .build();
    }

    @Test
    public void headObject503SlowDown_shouldBeThrottlingException() {
        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(503).withStatusMessage("Slow Down")));

        assertThatThrownBy(() -> client.headObject(r -> r.bucket("bucket").key("key")))
            .isInstanceOfSatisfying(S3Exception.class, e -> {
                assertThat(e.statusCode()).isEqualTo(503);
                assertThat(e.isThrottlingException()).isTrue();
                assertThat(e.awsErrorDetails().errorCode()).isEqualTo("SlowDown");
            });
    }

    @Test
    public void headBucket503SlowDown_shouldBeThrottlingException() {
        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(503).withStatusMessage("Slow Down")));

        assertThatThrownBy(() -> client.headBucket(r -> r.bucket("bucket")))
            .isInstanceOfSatisfying(S3Exception.class, e -> {
                assertThat(e.statusCode()).isEqualTo(503);
                assertThat(e.isThrottlingException()).isTrue();
                assertThat(e.awsErrorDetails().errorCode()).isEqualTo("SlowDown");
            });
    }

    @Test
    public void headObject503OtherException_shouldNotBeThrottlingException() {
        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(503).withStatusMessage("Service Unavailable")));

        assertThatThrownBy(() -> client.headObject(r -> r.bucket("bucket").key("key")))
            .isInstanceOfSatisfying(S3Exception.class, e -> {
                assertThat(e.statusCode()).isEqualTo(503);
                assertThat(e.isThrottlingException()).isFalse();
                assertThat(e.awsErrorDetails().errorCode()).isNull();
            });
    }
}
