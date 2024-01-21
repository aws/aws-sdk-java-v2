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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    private S3AsyncClient s3AsyncClient;

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
        CrtResource.waitForNoResources();
    }

    @Test
    public void completeMultipartUpload_completeResponse() {
        String location = "http://Example-Bucket.s3.amazonaws.com/Example-Object";
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String eTag = "\"3858f62230ac3c915f300c664312c11f-9\"";
        String xmlResponseBody = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<CompleteMultipartUploadResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">\n"
            + "<Location>%s</Location>\n"
            + "<Bucket>%s</Bucket>\n"
            + "<Key>%s</Key>\n"
            + "<ETag>%s</ETag>\n"
            + "</CompleteMultipartUploadResult>", location, bucket, key, eTag);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(xmlResponseBody)));

        CompleteMultipartUploadResponse response = s3AsyncClient.completeMultipartUpload(
            r -> r.bucket(bucket).key(key).uploadId("upload-id")).join();

        assertThat(response.location()).isEqualTo(location);
        assertThat(response.bucket()).isEqualTo(bucket);
        assertThat(response.key()).isEqualTo(key);
        assertThat(response.eTag()).isEqualTo(eTag);
    }
}
