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

package software.amazon.awssdk.services.s3.checksums;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.async.AsyncResponseTransformer.toBytes;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Verifies that the checksum validators are reset on an HTTP retry.
 */
public class ChecksumResetsOnRetryTest {
    @Rule
    public WireMockRule mockServer = new WireMockRule(new WireMockConfiguration().port(0)
                                                                                 .notifier(new ConsoleNotifier(true)));

    private S3Client s3Client;

    private S3AsyncClient s3AsyncClient;

    private byte[] body;

    private byte[] bodyWithTrailingChecksum;

    private String bodyEtag;

    @Before
    public void setup() {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
        s3Client = S3Client.builder()
                           .credentialsProvider(credentials)
                           .region(Region.US_WEST_2)
                           .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                           .build();

        s3AsyncClient = S3AsyncClient.builder()
                                     .credentialsProvider(credentials)
                                     .region(Region.US_WEST_2)
                                     .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                                     .build();

        body = "foo".getBytes(StandardCharsets.UTF_8);
        String checksumAsHexString = "acbd18db4cc2f85cedef654fccc4a4d8";
        bodyEtag = "\"" + checksumAsHexString + "\"";
        bodyWithTrailingChecksum = ArrayUtils.addAll(body, BinaryUtils.fromHex(checksumAsHexString));
    }

    @Test
    public void syncPutObject_resetsChecksumOnRetry() {
        stubSuccessAfterOneRetry(r -> r.withHeader("ETag", bodyEtag));

        PutObjectResponse response = s3Client.putObject(r -> r.bucket("foo").key("bar"), RequestBody.fromBytes(body));
        assertThat(response.eTag()).isEqualTo(bodyEtag);
    }

    @Test
    public void asyncPutObject_resetsChecksumOnRetry() {
        stubSuccessAfterOneRetry(r -> r.withHeader("ETag", bodyEtag));

        PutObjectResponse response = s3AsyncClient.putObject(r -> r.bucket("foo").key("bar"), AsyncRequestBody.fromBytes(body)).join();
        assertThat(response.eTag()).isEqualTo(bodyEtag);
    }

    @Test
    public void syncGetObject_resetsChecksumOnRetry() {
        stubSuccessAfterOneRetry(r -> r.withHeader("ETag", bodyEtag)
                                       .withHeader("x-amz-transfer-encoding", "append-md5")
                                       .withHeader("content-length", Integer.toString(bodyWithTrailingChecksum.length))
                                       .withBody(bodyWithTrailingChecksum));

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(r -> r.bucket("foo").key("bar"));
        assertThat(response.response().eTag()).isEqualTo(bodyEtag);
        assertThat(response.asByteArray()).isEqualTo(body);
    }

    @Test
    public void asyncGetObject_resetsChecksumOnRetry() {
        stubSuccessAfterOneRetry(r -> r.withHeader("ETag", bodyEtag)
                                       .withHeader("x-amz-transfer-encoding", "append-md5")
                                       .withHeader("content-length", Integer.toString(bodyWithTrailingChecksum.length))
                                       .withBody(bodyWithTrailingChecksum));

        ResponseBytes<GetObjectResponse> response = s3AsyncClient.getObject(r -> r.bucket("foo").key("bar"), toBytes()).join();
        assertThat(response.response().eTag()).isEqualTo(bodyEtag);
        assertThat(response.asByteArray()).isEqualTo(body);
    }

    private void stubSuccessAfterOneRetry(Consumer<ResponseDefinitionBuilder> successfulResponseModifier) {
        WireMock.reset();

        String scenario = "stubSuccessAfterOneRetry";
        stubFor(any(anyUrl())
                    .willReturn(aResponse().withStatus(500).withBody("<xml></xml>"))
                    .inScenario(scenario)
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("200"));

        ResponseDefinitionBuilder successfulResponse = aResponse().withStatus(200).withBody("<xml></xml>");
        successfulResponseModifier.accept(successfulResponse);
        stubFor(any(anyUrl())
                    .willReturn(successfulResponse)
                    .inScenario(scenario)
                    .whenScenarioStateIs("200"));
    }
}

