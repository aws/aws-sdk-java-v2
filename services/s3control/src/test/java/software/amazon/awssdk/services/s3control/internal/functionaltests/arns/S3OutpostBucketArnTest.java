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
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

public class S3OutpostBucketArnTest extends S3ControlWireMockTestBase {
    private S3ControlClient s3Control;

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private static final String URL_PREFIX = "/v20180820/bucket/";
    private static final String EXPECTED_HOST = "s3-outposts.%s.amazonaws.com";

    @Before
    public void methodSetUp() {
        s3Control = buildClient();
    }

    @Test
    public void malformedArn_MissingBucketSegment_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().build();

        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456";

        exception.expect(SdkClientException.class);
        exception.expectMessage("Invalid ARN: Expected a 4-component resource");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void malformedArn_missingOutpostId_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().build();

        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost";

        exception.expect(SdkClientException.class);
        exception.expectMessage("Invalid ARN: The Outpost Id was not set");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void malformedArn_missingOutpostIdAndBucketName_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().build();

        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:bucket";

        exception.expect(SdkClientException.class);
        exception.expectMessage("Invalid ARN: Expected a 4-component resource");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void malformedArn_missingBucketName_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().build();

        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket";

        exception.expect(SdkClientException.class);
        exception.expectMessage("Invalid ARN: expected a bucket name");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void bucketArnDifferentRegionNoConfigFlag_throwsIllegalArgumentException() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-west-2")).build();
        String bucketArn = "arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        exception.expect(SdkClientException.class);
        exception.expectMessage("Invalid configuration: region from ARN `us-east-1` does not match client region `us-west-2` and UseArnRegion is `false`");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void bucketArnInvalidPartition_throwsIllegalArgumentException() {
        S3ControlClient s3ControlForTest =
            initializedBuilder().region(Region.of("us-west-2")).serviceConfiguration(b -> b.useArnRegionEnabled(true)).build();
        String bucketArn = "arn:aws-cn:s3-outposts:cn-north-1:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        exception.expect(SdkClientException.class);
        exception.expectMessage("Client was configured for partition `aws` but ARN has `aws-cn`");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void bucketArn_conflictingAccountIdPresent_shouldThrowException() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-west-2")).build();
        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        exception.expect(SdkClientException.class);
        exception.expectMessage("Invalid ARN: the accountId specified in the ARN (`123456789012`) does not match the parameter (`1234`)");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn).accountId("1234"));
    }

    @Test
    public void bucketArnUSRegion() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-west-2")).build();

        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        String expectedUrl = getUrl(bucketArn);
        stubResponse(expectedUrl);

        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
        verifyRequest("us-west-2", expectedUrl);
    }

    @Test
    public void bucketArn_GovRegion() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-gov-east-1")).build();

        String bucketArn = "arn:aws-us-gov:s3-outposts:us-gov-east-1:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        String expectedUrl = getUrl(bucketArn);
        stubResponse(expectedUrl);

        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));

        verifyRequest("us-gov-east-1", expectedUrl);
    }

    @Test
    public void bucketArn_futureRegion_US() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-future-1")).build();

        String bucketArn = "arn:aws:s3-outposts:us-future-1:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        String expectedUrl = getUrl(bucketArn);
        stubResponse(expectedUrl);

        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));

        verifyRequest("us-future-1", expectedUrl);
    }

    @Test
    public void bucketArn_futureRegion_CN() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("cn-future-1")).build();
        String bucketArn = "arn:aws-cn:s3-outposts:cn-future-1:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        String expectedUrl = getUrl(bucketArn);
        stubResponse(expectedUrl);

        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
        verifyOutpostRequest("cn-future-1", expectedUrl, "s3-outposts.cn-future-1.amazonaws.com.cn");
    }

    @Test
    public void bucketArnDifferentRegion_useArnRegionSet_shouldUseRegionFromArn() {
        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        String expectedUrl = getUrl(bucketArn);
        stubResponse(expectedUrl);

        S3ControlClient s3WithUseArnRegion =
            initializedBuilder().region(Region.of("us-east-1")).serviceConfiguration(b -> b.useArnRegionEnabled(true)).build();

        s3WithUseArnRegion.getBucket(b -> b.bucket(bucketArn));

        verifyRequest("us-west-2", expectedUrl);
    }


    @Test
    public void outpostBucketArn_listAccessPoints() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-west-2")).build();

        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        String expectedUrl = "/v20180820/accesspoint?bucket=" + SdkHttpUtils.urlEncode(bucketArn);

        stubFor(get(urlEqualTo(expectedUrl)).willReturn(aResponse().withBody("<xml></xml>").withStatus(200)));

        s3ControlForTest.listAccessPoints(b -> b.bucket(bucketArn));
        verify(getRequestedFor(urlEqualTo(expectedUrl))
                   .withHeader("authorization", containing("us-west-2/s3-outposts/aws4_request"))
                   .withHeader("x-amz-outpost-id", equalTo("op-01234567890123456"))
                   .withHeader("x-amz-account-id", equalTo("123456789012")));
        assertThat(getRecordedEndpoints().size(), is(1));
        assertThat(getRecordedEndpoints().get(0).getHost(), is(String.format(EXPECTED_HOST, "us-west-2")));
    }

    @Test
    public void outpostBucketArn_createAccessPoint() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-west-2")).build();

        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        stubFor(put(urlEqualTo("/v20180820/accesspoint/name")).willReturn(aResponse().withBody("<xml></xml>").withStatus(200)));

        String bucketResponseXml = String.format("<Bucket>%s</Bucket>", bucketArn);

        s3ControlForTest.createAccessPoint(b -> b.bucket(bucketArn).name("name"));
        verify(putRequestedFor(urlEqualTo("/v20180820/accesspoint/name"))
                   .withRequestBody(containing(bucketResponseXml))
                   .withHeader("authorization", containing("us-west-2/s3-outposts/aws4_request"))
                   .withHeader("x-amz-outpost-id", equalTo("op-01234567890123456"))
                   .withHeader("x-amz-account-id", equalTo("123456789012")));
        assertThat(getRecordedEndpoints().size(), is(1));
        assertThat(getRecordedEndpoints().get(0).getHost(), is(String.format(EXPECTED_HOST, "us-west-2")));
    }

    private void verifyRequest(String region, String expectedUrl) {
        verifyOutpostRequest(region, expectedUrl, String.format(EXPECTED_HOST, region));
    }

    private String getUrl(String arn) {
        return URL_PREFIX + SdkHttpUtils.urlEncode(arn);
    }
}
