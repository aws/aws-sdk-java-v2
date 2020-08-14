/*
 * Copyright 2011-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.services.s3control.internal.functionaltests.arns;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.util.List;
import org.junit.Rule;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.S3ControlClientBuilder;

/**
 * Base class for tests that use a WireMock server
 */
public abstract class S3ControlWireMockTestBase {
    private S3ControlWireMockRerouteInterceptor s3ControlWireMockRequestHandler;

    @Rule
    public WireMockRule mockServer = new WireMockRule(new WireMockConfiguration().port(0).httpsPort(0));

    protected String getEndpoint() {
        return "http://localhost:" + mockServer.port();
    }

    protected S3ControlClient buildClient() {
        this.s3ControlWireMockRequestHandler = new S3ControlWireMockRerouteInterceptor(URI.create(getEndpoint()));

        return initializedBuilder().build();
    }

    protected S3ControlClientBuilder buildClientCustom() {
        this.s3ControlWireMockRequestHandler = new S3ControlWireMockRerouteInterceptor(URI.create(getEndpoint()));

        return initializedBuilder();
    }

    protected S3ControlClient buildClientWithCustomEndpoint(String serviceEndpoint, String signingRegion) {
        this.s3ControlWireMockRequestHandler = new S3ControlWireMockRerouteInterceptor(URI.create(getEndpoint()));
        return initializedBuilder().region(Region.of(signingRegion)).endpointOverride(URI.create(serviceEndpoint)).build();
    }

    protected S3ControlClientBuilder initializedBuilder() {
        return S3ControlClient.builder()
                              .credentialsProvider(() -> AwsBasicCredentials.create("test", "test"))
                              .region(Region.US_WEST_2)
                              .overrideConfiguration(o -> o.addExecutionInterceptor(this.s3ControlWireMockRequestHandler));
    }

    protected List<SdkHttpRequest> getRecordedRequests() {
        return this.s3ControlWireMockRequestHandler.getRecordedRequests();
    }

    protected List<URI> getRecordedEndpoints() {
        return this.s3ControlWireMockRequestHandler.getRecordedEndpoints();
    }

    protected void verifyOutpostRequest(String region, String expectedHost) {
        verify(getRequestedFor(urlEqualTo(expectedUrl()))
                   .withHeader("Authorization", containing(String.format("%s/s3-outposts/aws4_request", region)))
                   .withHeader("x-amz-outpost-id", equalTo("op-01234567890123456"))
                   .withHeader("x-amz-account-id", equalTo("123456789012")));
        assertThat(getRecordedEndpoints().size(), is(1));
        assertThat(getRecordedEndpoints().get(0).getHost(), is(expectedHost));
    }

    protected void stubResponse() {
        stubFor(get(urlMatching(expectedUrl())).willReturn(aResponse().withBody("<xml></xml>").withStatus(200)));
    }

    protected void verifyS3ControlRequest(String region, String expectedHost) {
        verify(getRequestedFor(urlEqualTo(expectedUrl())).withHeader("Authorization", containing(String.format("%s/s3/aws4_request", region)))
                                                         .withHeader("x-amz-account-id", equalTo("123456789012")));
        assertThat(getRecordedEndpoints().size(), is(1));
        assertThat(getRecordedEndpoints().get(0).getHost(), is(expectedHost));
    }

    abstract String expectedUrl();
}
