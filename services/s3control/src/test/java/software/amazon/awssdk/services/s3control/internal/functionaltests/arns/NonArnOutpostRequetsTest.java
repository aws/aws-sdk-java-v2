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

package software.amazon.awssdk.services.s3control.internal.functionaltests.arns;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3control.S3ControlClient;


public class NonArnOutpostRequetsTest extends S3ControlWireMockTestBase {
    private S3ControlClient s3;
    private static final String EXPECTED_URL = "/v20180820/bucket";

    @Before
    public void methodSetUp() {
        s3 = buildClient();
    }

    @Test
    public void listRegionalBuckets_outpostIdNotNull_shouldRedirect() {
        S3ControlClient s3Control = initializedBuilder().region(Region.of("us-west-2")).build();
        stubFor(get(urlMatching("/v20180820/bucket")).willReturn(aResponse().withBody("<xml></xml>").withStatus(200)));

        s3Control.listRegionalBuckets(b -> b.outpostId("op-01234567890123456").accountId("123456789012"));
        String expectedHost = "s3-outposts.us-west-2.amazonaws.com";
        verifyOutpostRequest("us-west-2", expectedHost);
    }

    @Test
    public void listRegionalBuckets_outpostIdNull_shouldNotRedirect() {
        S3ControlClient s3Control = initializedBuilder().region(Region.of("us-west-2")).build();
        stubFor(get(urlMatching("/v20180820/bucket")).willReturn(aResponse().withBody("<xml></xml>").withStatus(200)));

        s3Control.listRegionalBuckets(b -> b.accountId("123456789012"));
        String expectedHost = "123456789012.s3-control.us-west-2.amazonaws.com";
        verifyS3ControlRequest("us-west-2", expectedHost);
    }

    @Test
    public void createBucketRequest_outpostIdNotNull_shouldRedirect() {
        S3ControlClient s3Control = initializedBuilder().region(Region.of("us-west-2")).build();
        stubFor(put(urlMatching("/v20180820/bucket/test")).willReturn(aResponse().withBody("<xml></xml>").withStatus(200)));

        s3Control.createBucket(b -> b.outpostId("op-01234567890123456").bucket("test"));
        String expectedHost = "s3-outposts.us-west-2.amazonaws.com";
        verify(putRequestedFor(urlEqualTo("/v20180820/bucket/test"))
                   .withHeader("Authorization", containing("us-west-2/s3-outposts/aws4_request"))
                   .withHeader("x-amz-outpost-id", equalTo("op-01234567890123456")));
        assertThat(getRecordedEndpoints().size(), is(1));
        assertThat(getRecordedEndpoints().get(0).getHost(), is(expectedHost));
    }

    @Test
    public void createBucketRequest_outpostIdNull_shouldNotRedirect() {
        S3ControlClient s3Control = initializedBuilder().region(Region.of("us-west-2")).build();
        stubFor(put(urlMatching("/v20180820/bucket/test")).willReturn(aResponse().withBody("<xml></xml>").withStatus(200)));

        s3Control.createBucket(b -> b.bucket("test"));
        String expectedHost = "s3-control.us-west-2.amazonaws.com";

        verify(putRequestedFor(urlEqualTo("/v20180820/bucket/test")).withHeader("Authorization", containing("us-west-2/s3/aws4_request")));
        assertThat(getRecordedEndpoints().size(), is(1));
        assertThat(getRecordedEndpoints().get(0).getHost(), is(expectedHost));
    }


    @Override
    String expectedUrl() {
        return EXPECTED_URL;
    }
}
