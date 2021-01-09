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
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

public class MultipartUploadTest {

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    private S3Client s3Client;

    private S3AsyncClient s3AsyncClient;

    @Before
    public void setup() {
        s3Client = S3Client.builder()
                           .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                           .region(Region.US_WEST_2).endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                           .serviceConfiguration(S3Configuration.builder().checksumValidationEnabled(false).build())
                           .build();

        s3AsyncClient = S3AsyncClient.builder()
                                     .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid",
                                                                                                                      "skid")))
                                     .region(Region.US_WEST_2).endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                                     .serviceConfiguration(S3Configuration.builder().checksumValidationEnabled(false).build())
                                     .build();
    }

    @Test
    public void syncCreateMultipartUpload_shouldHaveUploadsQueryParam() {
        stubFor(any(urlMatching(".*"))
                    .willReturn(aResponse().withStatus(200).withBody("<xml></xml>")));
        s3Client.createMultipartUpload(b -> b.key("key").bucket("bucket"));

        verify(anyRequestedFor(anyUrl()).withQueryParam("uploads", containing("")));
        verify(anyRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo("binary/octet-stream")));
    }

    @Test
    public void asyncCreateMultipartUpload_shouldHaveUploadsQueryParam() {
        stubFor(any(urlMatching(".*"))
                    .willReturn(aResponse().withStatus(200).withBody("<xml></xml>")));
        s3AsyncClient.createMultipartUpload(b -> b.key("key").bucket("bucket")).join();

        verify(anyRequestedFor(anyUrl()).withQueryParam("uploads", containing("")));
        verify(anyRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo("binary/octet-stream")));
    }

    @Test
    public void createMultipartUpload_overrideContentType() {
        String overrideContentType = "application/html";
        stubFor(any(urlMatching(".*"))
                    .willReturn(aResponse().withStatus(200).withBody("<xml></xml>")));
        s3Client.createMultipartUpload(b -> b.key("key")
                                             .bucket("bucket")
                                             .overrideConfiguration(c -> c.putHeader(CONTENT_TYPE, overrideContentType)));

        verify(anyRequestedFor(anyUrl()).withQueryParam("uploads", containing("")));
        verify(anyRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo(overrideContentType)));
    }
}
