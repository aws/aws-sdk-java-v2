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

package software.amazon.awssdk.services.s3.internal.crt;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;

/**
 * Tests to make sure all CRT resources are cleaned up
 */
@WireMockTest
public class S3CrtClientWiremockTest {

    private static final String LOCATION = "http://Example-Bucket.s3.amazonaws.com/Example-Object";
    private static final String BUCKET = "Example-Bucket";
    private static final String KEY = "Example-Object";
    private static final String E_TAG = "\"3858f62230ac3c915f300c664312c11f-9\"";
    private static final String XML_RESPONSE_BODY = String.format(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<CompleteMultipartUploadResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">\n"
        + "<Location>%s</Location>\n"
        + "<Bucket>%s</Bucket>\n"
        + "<Key>%s</Key>\n"
        + "<ETag>%s</ETag>\n"
        + "</CompleteMultipartUploadResult>", LOCATION, BUCKET, KEY, E_TAG);
    private S3AsyncClient s3AsyncClient;
    private S3AsyncClient clientWithCustomExecutor;
    private SpyableExecutor mockExecutor;

    @BeforeAll
    public static void setUpBeforeAll() {
        System.setProperty("aws.crt.debugnative", "true");
        Log.initLoggingToStdout(Log.LogLevel.Warn);
    }

    @BeforeEach
    public void setup(WireMockRuntimeInfo wiremock) {
        s3AsyncClient = S3AsyncClient.crtBuilder()
                                     .region(Region.US_EAST_1)
                                     .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                     .credentialsProvider(
                                         StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                                     .build();
    }

    @AfterEach
    public void tearDown() {
        s3AsyncClient.close();
    }

    @AfterAll
    public static void verifyCrtResource() {
        CrtResource.waitForNoResources();
    }

    @Test
    public void completeMultipartUpload_completeResponse() {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(XML_RESPONSE_BODY)));

        CompleteMultipartUploadResponse response = s3AsyncClient.completeMultipartUpload(
            r -> r.bucket(BUCKET).key(KEY).uploadId("upload-id")).join();

        assertThat(response.location()).isEqualTo(LOCATION);
        assertThat(response.bucket()).isEqualTo(BUCKET);
        assertThat(response.key()).isEqualTo(KEY);
        assertThat(response.eTag()).isEqualTo(E_TAG);
    }

    @Test
    void overrideResponseCompletionExecutor_shouldCompleteWithCustomExecutor(WireMockRuntimeInfo wiremock) {

        mockExecutor = Mockito.spy(new SpyableExecutor());

        try (S3AsyncClient s3AsyncClient = S3AsyncClient.crtBuilder()
                                                        .region(Region.US_EAST_1)
                                                        .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                                        .futureCompletionExecutor(mockExecutor)
                                                        .credentialsProvider(
                                                            StaticCredentialsProvider.create(AwsBasicCredentials.create("key",
                                                                                                                        "secret")))
                                                        .build()) {
            stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(XML_RESPONSE_BODY)));

            CompleteMultipartUploadResponse response = s3AsyncClient.completeMultipartUpload(
                r -> r.bucket(BUCKET).key(KEY).uploadId("upload-id")).join();

            verify(mockExecutor).execute(any(Runnable.class));
        }
    }

    private static class SpyableExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
