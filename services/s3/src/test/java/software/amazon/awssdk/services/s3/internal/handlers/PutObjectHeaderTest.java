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

package software.amazon.awssdk.services.s3.internal.handlers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
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
                           .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                           .region(Region.US_WEST_2).endpointOverride(URI.create(getEndpoint()))
                           .serviceConfiguration(S3Configuration.builder().checksumValidationEnabled(false).build())
                           .build();
        putObjectRequest = PutObjectRequest.builder().bucket("test").key("test").build();
    }

    private String getEndpoint() {
        return "http://localhost:" + mockServer.port();
    }

    @Test
    public void putObjectBytes_headerShouldContainContentType() {
        stubFor(any(urlMatching(".*"))
                    .willReturn(response()));
        s3Client.putObject(PutObjectRequest.builder().bucket("test").key("test").build(), RequestBody.fromBytes("Hello World".getBytes()));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo(Mimetype.MIMETYPE_OCTET_STREAM)));
    }

    @Test
    public void putObjectFile_headerShouldContainContentType() throws IOException {
        File file = new RandomTempFile("test.html", 10);
        stubFor(any(urlMatching(".*"))
                    .willReturn(response()));
        s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo("text/html")));
    }

    @Test
    public void putObject_gzipFile_hasProperContentType() throws IOException {
        File file = new RandomTempFile("test.gz", 10);
        stubFor(any(urlMatching(".*"))
                    .willReturn(response()));
        s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo("application/gzip")));
    }

    @Test
    public void putObject_gzipFile_shouldNotOverrideContentTypeInRequest() throws IOException {
        File file = new RandomTempFile("test.gz", 10);
        String contentType = "something";
        stubFor(any(urlMatching(".*"))
                    .willReturn(response()));

        s3Client.putObject(putObjectRequest.toBuilder().contentType(contentType).build(),
                           RequestBody.fromFile(file));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo(contentType)));
    }

    @Test
    public void putObjectUnknownFileExtension_contentTypeDefaultToBeStream() throws IOException {
        File file = new RandomTempFile("test.unknown", 10);
        stubFor(any(urlMatching(".*"))
                    .willReturn(response()));
        s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo(Mimetype.MIMETYPE_OCTET_STREAM)));
    }

    @Test
    public void putObjectWithContentTypeHeader_shouldNotOverrideContentTypeInRequest() throws IOException {
        stubFor(any(urlMatching(".*"))
                    .willReturn(response()));
        String contentType = "something";
        putObjectRequest = putObjectRequest.toBuilder().contentType(contentType).build();
        s3Client.putObject(putObjectRequest, RequestBody.fromString("test"));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo(contentType)));
    }

    @Test
    public void putObjectWithContentTypeHeader_shouldNotOverrideContentTypeInRawConfig() throws IOException {
        stubFor(any(urlMatching(".*"))
                    .willReturn(response()));
        String contentType = "hello world";

        putObjectRequest = (PutObjectRequest) putObjectRequest.toBuilder().overrideConfiguration(b -> b.putHeader(CONTENT_TYPE, contentType)).build();
        s3Client.putObject(putObjectRequest, RequestBody.fromString("test"));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo(contentType)));
    }

    @Test
    public void headObject_userMetadataReturnMixedCaseMetadata() {
        String lowerCaseMetadataPrefix = "x-amz-meta-";
        String mixedCaseMetadataPrefix = "X-AmZ-MEta-";
        String metadataKey = "foo";
        String mixedCaseMetadataKey = "bAr";

        stubFor(any(urlMatching(".*"))
                    .willReturn(response().withHeader(lowerCaseMetadataPrefix + metadataKey, "test")
                                          .withHeader(mixedCaseMetadataPrefix + mixedCaseMetadataKey, "test")));
        HeadObjectResponse headObjectResponse = s3Client.headObject(b -> b.key("key").bucket("bucket"));

        assertThat(headObjectResponse.metadata()).containsKey(metadataKey);
        assertThat(headObjectResponse.metadata()).containsKey(mixedCaseMetadataKey);
    }

    private ResponseDefinitionBuilder response() {
        return aResponse().withStatus(200).withHeader(CONTENT_LENGTH, "0").withBody("");
    }
}
