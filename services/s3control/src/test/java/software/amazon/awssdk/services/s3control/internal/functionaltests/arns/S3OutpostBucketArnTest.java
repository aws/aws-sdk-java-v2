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
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3control.S3ControlClient;

public class S3OutpostBucketArnTest extends S3ControlWireMockTestBase {
    private S3ControlClient s3Control;

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private static final String EXPECTED_URL = "/v20180820/bucket/mybucket";
    private static final String EXPECTED_HOST = "s3-outposts.%s.amazonaws.com";

    @Before
    public void methodSetUp() {
        s3Control = buildClient();
    }

    @Test
    public void fipsEnabledInConfig_shouldThrowException() {
        S3ControlClient s3ControlForTest =
            buildClientCustom().region(Region.of("us-gov-east-1")).serviceConfiguration(b -> b.fipsModeEnabled(true)).build();

        String bucketArn = "arn:aws-us-gov:s3-outposts:fips-us-gov-east-1:123456789012:outpost:op-01234567890123456:bucket"
                           + ":mybucket";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("FIPS");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void fipsRegionProvided_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().region(Region.of("fips-us-gov-east-1")).build();

        String bucketArn = "arn:aws-us-gov:s3-outposts:us-gov-east-1:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("FIPS");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void dualstackEnabled_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().serviceConfiguration(b -> b.dualstackEnabled(true)).build();

        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Dualstack");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void malformedArn_MissingBucketSegment_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().serviceConfiguration(b -> b.dualstackEnabled(true)).build();

        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid format");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void malformedArn_missingOutpostId_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().serviceConfiguration(b -> b.dualstackEnabled(true)).build();

        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Unknown ARN type");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void malformedArn_missingOutpostIdAndBucketName_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().serviceConfiguration(b -> b.dualstackEnabled(true)).build();

        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:bucket";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid format");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void malformedArn_missingBucketName_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().serviceConfiguration(b -> b.dualstackEnabled(true)).build();

        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid format");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void bucketArnDifferentRegionNoConfigFlag_throwsIllegalArgumentException() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-west-2")).build();
        String bucketArn = "arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("does not match the region the client was configured with");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void bucketArnInvalidPartition_throwsIllegalArgumentException() {
        S3ControlClient s3ControlForTest =
            initializedBuilder().region(Region.of("us-west-2")).serviceConfiguration(b -> b.useArnRegionEnabled(true)).build();
        String bucketArn = "arn:aws-cn:s3-outposts:cn-north-1:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("does not match the partition the client has been configured with");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void bucketArnWithCustomEndpoint_throwsIllegalArgumentException() {
        S3ControlClient s3ControlForTest = buildClientWithCustomEndpoint("https://foo.bar", "us-west-2");
        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("has been configured with an endpoint override");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
    }

    @Test
    public void bucketArn_conflictingAccountIdPresent_shouldThrowException() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-west-2")).build();
        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("accountId");
        s3ControlForTest.getBucket(b -> b.bucket(bucketArn).accountId("1234"));
    }

    @Test
    public void bucketArnUSRegion() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-west-2")).build();

        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        stubResponse();

        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
        verifyRequest("us-west-2");
    }

    @Test
    public void bucketArn_GovRegion() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-gov-east-1")).build();

        String bucketArn = "arn:aws-us-gov:s3-outposts:us-gov-east-1:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        stubResponse();

        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));

        verifyRequest("us-gov-east-1");
    }

    @Test
    public void bucketArn_futureRegion_US() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-future-1")).build();

        String bucketArn = "arn:aws:s3-outposts:us-future-1:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        stubResponse();

        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));

        verifyRequest("us-future-1");
    }

    @Test
    public void bucketArn_futureRegion_CN() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("cn-future-1")).build();
        String bucketArn = "arn:aws-cn:s3-outposts:cn-future-1:123456789012:outpost:op-01234567890123456:bucket:mybucket";
        stubResponse();

        s3ControlForTest.getBucket(b -> b.bucket(bucketArn));
        verifyOutpostRequest("cn-future-1", "s3-outposts.cn-future-1.amazonaws.com.cn");
    }

    @Test
    public void bucketArnDifferentRegion_useArnRegionSet_shouldUseRegionFromArn() {
        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket";
        stubResponse();

        S3ControlClient s3WithUseArnRegion =
            initializedBuilder().region(Region.of("us-east-1")).serviceConfiguration(b -> b.useArnRegionEnabled(true)).build();

        s3WithUseArnRegion.getBucket(b -> b.bucket(bucketArn));

        verifyRequest("us-west-2");
    }

    @Test
    public void fipsClientRegion_bucketArnDifferentRegion_useArnRegionSet_shouldUseRegionFromArn() {
        String bucketArn = "arn:aws-us-gov:s3-outposts:us-gov-east-1:123456789012:outpost:op-01234567890123456:bucket:mybucket";
        stubResponse();

        S3ControlClient s3WithUseArnRegion =
            initializedBuilder().region(Region.of("fips-us-gov-east-1")).serviceConfiguration(b -> b.useArnRegionEnabled(true)).build();

        s3WithUseArnRegion.getBucket(b -> b.bucket(bucketArn));

        verifyRequest("us-gov-east-1");
    }

    @Test
    public void outpostBucketArn_listAccessPoints() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-west-2")).build();

        String bucketArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket";

        stubFor(get(urlEqualTo("/v20180820/accesspoint?bucket=mybucket")).willReturn(aResponse().withBody("<xml></xml>").withStatus(200)));

        s3ControlForTest.listAccessPoints(b -> b.bucket(bucketArn));
        verify(getRequestedFor(urlEqualTo("/v20180820/accesspoint?bucket=mybucket"))
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

        s3ControlForTest.createAccessPoint(b -> b.bucket(bucketArn).name("name"));
        verify(putRequestedFor(urlEqualTo("/v20180820/accesspoint/name"))
                   .withRequestBody(containing("<Bucket>mybucket</Bucket"))
                   .withHeader("authorization", containing("us-west-2/s3-outposts/aws4_request"))
                   .withHeader("x-amz-outpost-id", equalTo("op-01234567890123456"))
                   .withHeader("x-amz-account-id", equalTo("123456789012")));
        assertThat(getRecordedEndpoints().size(), is(1));
        assertThat(getRecordedEndpoints().get(0).getHost(), is(String.format(EXPECTED_HOST, "us-west-2")));
    }

    @Override
    String expectedUrl() {
        return EXPECTED_URL;
    }

    private void verifyRequest(String region) {
        verifyOutpostRequest(region, String.format(EXPECTED_HOST, region));
    }
}
