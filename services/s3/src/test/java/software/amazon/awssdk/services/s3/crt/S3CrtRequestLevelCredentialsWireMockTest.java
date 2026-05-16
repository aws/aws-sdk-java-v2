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

package software.amazon.awssdk.services.s3.crt;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * WireMock tests verifying that request-level credential overrides are used for signing
 * with the S3 CRT client. Verifies the Authorization header contains the expected access key.
 */
@WireMockTest
@Timeout(10)
public class S3CrtRequestLevelCredentialsWireMockTest {

    private static final String BUCKET = "my-bucket";
    private static final String KEY = "my-key";
    private static final String PATH = String.format("/%s/%s", BUCKET, KEY);
    private static final byte[] CONTENT = "hello".getBytes(StandardCharsets.UTF_8);

    private static final StaticCredentialsProvider CLIENT_CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("clientAccessKey", "clientSecretKey"));

    private static final StaticCredentialsProvider REQUEST_CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("requestAccessKey", "requestSecretKey"));

    private S3AsyncClient s3;

    @BeforeAll
    public static void setUpBeforeAll() {
        System.setProperty("aws.crt.debugnative", "true");
        Log.initLoggingToStdout(Log.LogLevel.Warn);
    }

    @BeforeEach
    public void setup(WireMockRuntimeInfo wiremock) {
        stubFor(head(urlPathEqualTo(PATH))
                    .willReturn(WireMock.aResponse().withStatus(200)
                                        .withHeader("ETag", "etag")
                                        .withHeader("Content-Length",
                                                    Integer.toString(CONTENT.length))));
        stubFor(get(urlPathEqualTo(PATH))
                    .willReturn(WireMock.aResponse().withStatus(200)
                                        .withHeader("Content-Type", "text/plain")
                                        .withBody(CONTENT)));
        stubFor(put(urlPathEqualTo(PATH))
                    .willReturn(WireMock.aResponse().withStatus(200)
                                        .withHeader("ETag", "etag")));

        s3 = S3AsyncClient.crtBuilder()
                          .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                          .credentialsProvider(CLIENT_CREDENTIALS)
                          .forcePathStyle(true)
                          .region(Region.US_EAST_1)
                          .build();
    }

    @AfterEach
    public void tearDown() {
        s3.close();
    }

    @Test
    void getObject_withRequestLevelCredentials_shouldSignWithOverrideCredentials() {
        s3.getObject(
            b -> b.bucket(BUCKET).key(KEY)
                  .overrideConfiguration(o -> o.credentialsProvider(REQUEST_CREDENTIALS)),
            AsyncResponseTransformer.toBytes()).join();

        verify(getRequestedFor(urlPathEqualTo(PATH))
                   .withHeader("Authorization", containing("Credential=requestAccessKey/")));
    }

    @Test
    void getObject_withoutRequestLevelCredentials_shouldSignWithClientCredentials() {
        s3.getObject(
            b -> b.bucket(BUCKET).key(KEY),
            AsyncResponseTransformer.toBytes()).join();

        verify(getRequestedFor(urlPathEqualTo(PATH))
                   .withHeader("Authorization", containing("Credential=clientAccessKey/")));
    }

    @Test
    void putObject_withRequestLevelCredentials_shouldSignWithOverrideCredentials() {
        s3.putObject(
            b -> b.bucket(BUCKET).key(KEY)
                  .overrideConfiguration(o -> o.credentialsProvider(REQUEST_CREDENTIALS)),
            AsyncRequestBody.fromString("hello")).join();

        verify(putRequestedFor(urlPathEqualTo(PATH))
                   .withHeader("Authorization", containing("Credential=requestAccessKey/")));
    }

    @Test
    void putObject_withoutRequestLevelCredentials_shouldSignWithClientCredentials() {
        s3.putObject(
            b -> b.bucket(BUCKET).key(KEY),
            AsyncRequestBody.fromString("hello")).join();

        verify(putRequestedFor(urlPathEqualTo(PATH))
                   .withHeader("Authorization", containing("Credential=clientAccessKey/")));
    }
}
