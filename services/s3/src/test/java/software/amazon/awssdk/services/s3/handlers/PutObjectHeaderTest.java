/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3.handlers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static software.amazon.awssdk.http.Headers.CONTENT_TYPE;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.StaticCredentialsProvider;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.util.Mimetypes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;

public class PutObjectHeaderTest {

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    private S3Client s3Client;

    private PutObjectRequest putObjectRequest;

    @Before
    public void setup() {
        s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsCredentials.create("akid", "skid")))
                .region(Region.US_WEST_2).endpointOverride(URI.create(getEndpoint()))
                .build();
        putObjectRequest = PutObjectRequest.builder().bucket("test").key("test").build();
    }

    private String getEndpoint() {
        return "http://localhost:" + mockServer.port();
    }

    @Test
    public void putObjectBytes_headerShouldContainContentType() {
        stubFor(any(urlMatching(".*"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}")));
        s3Client.putObject(PutObjectRequest.builder().bucket("test").key("test").build(), RequestBody.of("Hello World".getBytes()));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo(Mimetypes.MIMETYPE_OCTET_STREAM)));
    }

    @Test
    public void putObjectFile_headerShouldContainContentType() throws IOException {
        File file = new RandomTempFile("test.html", 10);
        stubFor(any(urlMatching(".*"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}")));
        s3Client.putObject(putObjectRequest, RequestBody.of(file));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo("text/html")));
    }

    @Test
    public void putObjectUnknownFileExtension_contentTypeDefaultToBeStream() throws IOException {
        File file = new RandomTempFile("test.unknown", 10);
        stubFor(any(urlMatching(".*"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}")));
        s3Client.putObject(putObjectRequest, RequestBody.of(file));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo(Mimetypes.MIMETYPE_OCTET_STREAM)));
    }

    @Test
    public void putObjectWithContentTypeHeader_shouldNotOverrideContentTypeInRequest() throws IOException {
        stubFor(any(urlMatching(".*"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}")));
        String contentType = "something";
        putObjectRequest = putObjectRequest.toBuilder().contentType(contentType).build();
        s3Client.putObject(putObjectRequest, RequestBody.of("test"));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo(contentType)));
    }

    @Test
    public void putObjectWithContentTypeHeader_shouldNotOverrideContentTypeInRawConfig() throws IOException {
        stubFor(any(urlMatching(".*"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}")));
        String contentType = "hello world";

        putObjectRequest = (PutObjectRequest) putObjectRequest.toBuilder().requestOverrideConfig(b -> b.header(CONTENT_TYPE, contentType)).build();
        s3Client.putObject(putObjectRequest, RequestBody.of("test"));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo(contentType)));
    }
}
