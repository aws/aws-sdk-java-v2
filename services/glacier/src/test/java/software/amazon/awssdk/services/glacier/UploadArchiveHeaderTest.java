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

package software.amazon.awssdk.services.glacier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.util.Mimetype;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.glacier.model.UploadArchiveRequest;
import software.amazon.awssdk.testutils.RandomTempFile;

public class UploadArchiveHeaderTest {

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    private GlacierClient glacier;

    private UploadArchiveRequest request;

    @Before
    public void setup() {
        glacier = GlacierClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                .region(Region.US_WEST_2).endpointOverride(URI.create(getEndpoint()))
                .build();
        request = UploadArchiveRequest.builder().vaultName("test").build();
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
        glacier.uploadArchive(request, RequestBody.fromBytes("test".getBytes()));
        verify(postRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo(Mimetype.MIMETYPE_OCTET_STREAM)));
    }

    @Test
    public void uploadArchiveFile_headerShouldContainContentType() throws IOException {
        File file = new RandomTempFile("test.tsv", 10);
        stubFor(any(urlMatching(".*"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}")));
        glacier.uploadArchive(request, RequestBody.fromFile(file));
        file.delete();
        verify(postRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo("text/tab-separated-values")));
    }

    @Test
    public void uploadArchiveFile_contentTypeShouldNotBeOverrideIfSet() throws IOException {
        stubFor(any(urlMatching(".*"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}")));

        request = (UploadArchiveRequest) request.toBuilder().overrideConfiguration(b -> b.header(CONTENT_TYPE, "test")).build();
        glacier.uploadArchive(request, RequestBody.fromBytes("test".getBytes()));
        verify(postRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo("test")));
    }
}
