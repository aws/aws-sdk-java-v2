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


import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.S3ControlClientBuilder;

public class S3AccessPointArnTest extends S3ControlWireMockTestBase {
    private S3ControlClient s3;
    private static final String EXPECTED_URL = "/v20180820/accesspoint/myendpoint";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void methodSetUp() {
        s3 = buildClient();
    }

    @Test
    public void malformedArn_MissingOutpostSegment_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().build();

        String accessPointArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Unknown ARN type");
        s3ControlForTest.getAccessPoint(b -> b.name(accessPointArn));
    }

    @Test
    public void malformedArn_MissingAccessPointSegment_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().build();

        String accessPointArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid format");
        s3ControlForTest.getAccessPoint(b -> b.name(accessPointArn));
    }

    @Test
    public void malformedArn_MissingAccessPointName_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().build();

        String accessPointArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:myaccesspoint";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid format");
        s3ControlForTest.getAccessPoint(b -> b.name(accessPointArn));
    }

    @Test
    public void accessPointArn_ClientHasCustomEndpoint_throwsIllegalArgumentException() {
        S3ControlClient s3Control = buildClientWithCustomEndpoint("https://foo.bar", "us-east-1");

        String accessPointArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint"
                                + ":myaccesspoint";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("endpoint");
        s3Control.getAccessPoint(b -> b.name(accessPointArn));
    }

    @Test
    public void bucketArnDifferentRegionNoConfigFlag_throwsIllegalArgumentException() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-east-1")).build();
        String accessPointArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint"
                                + ":myaccesspoint";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("does not match the region the client was configured with");
        s3ControlForTest.getAccessPoint(b -> b.name(accessPointArn));
    }

    @Test
    public void bucketArnDifferentPartition_throwsIllegalArgumentException() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-east-1")).build();

        String key = "test-path/test-file";
        String accessPointArn = "arn:bar:s3:us-east-1:12345678910:accesspoint:foobar";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("does not match the partition the client");
        s3ControlForTest.getAccessPoint(b -> b.name(accessPointArn));
    }

    @Test
    public void accessPointArn_accountIdPresent_shouldThrowException() {
        S3ControlClient s3Control = initializedBuilderForAccessPoint().region(Region.of("us-west-2")).build();

        String outpostArn = "arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("accountId");
        s3Control.getAccessPoint(b -> b.name(outpostArn).accountId("1234"));
    }

    @Test
    public void accessPointArnUSRegion() {
        S3ControlClient s3Control = initializedBuilderForAccessPoint().region(Region.of("us-west-2")).build();

        String accessPointArn = "arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint";
        String expectedHost = "123456789012.s3-control.us-west-2.amazonaws.com";

        stubResponse();
        s3Control.getAccessPoint(b -> b.name(accessPointArn));

        verifyS3ControlRequest("us-west-2", expectedHost);
    }

    @Test
    public void accessPointArn_GovRegion() {
        S3ControlClient s3Control = initializedBuilderForAccessPoint().region(Region.of("us-gov-east-1")).build();

        String accessPointArn = "arn:aws-us-gov:s3:us-gov-east-1:123456789012:accesspoint:myendpoint";
        String expectedHost = "123456789012.s3-control.us-gov-east-1.amazonaws.com";

        stubResponse();

        s3Control.getAccessPoint(b -> b.name(accessPointArn));

        verifyS3ControlRequest("us-gov-east-1", expectedHost);
    }

    @Test
    public void accessPointArn_futureRegion_US() {
        S3ControlClient s3Control = initializedBuilderForAccessPoint().region(Region.of("us-future-1")).build();
        String accessPointArn = "arn:aws:s3:us-future-1:123456789012:accesspoint:myendpoint";
        String expectedHost = "123456789012.s3-control.us-future-1.amazonaws.com";

        stubResponse();
        s3Control.getAccessPoint(b -> b.name(accessPointArn));

        verifyS3ControlRequest("us-future-1", expectedHost);
    }

    @Test
    public void accessPointArn_futureRegion_CN() {
        S3ControlClient s3Control = initializedBuilderForAccessPoint().region(Region.of("cn-future-1")).build();
        String accessPointArn = "arn:aws-cn:s3-outposts:cn-future-1:123456789012:accesspoint:myendpoint";

        String expectedHost = "123456789012.s3-control.cn-future-1.amazonaws.com.cn";

        stubResponse();

        s3Control.getAccessPoint(b -> b.name(accessPointArn));
        verifyS3ControlRequest("cn-future-1", expectedHost);
    }

    @Test
    public void accessPointArn_dualstackEnabled() {
        S3ControlClient s3Control =
            initializedBuilderForAccessPoint().serviceConfiguration(b -> b.dualstackEnabled(true)).build();

        String accessPointArn = "arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint";
        String expectedHost = "123456789012.s3-control.dualstack.us-west-2.amazonaws.com";

        stubResponse();
        s3Control.getAccessPoint(b -> b.name(accessPointArn));

        verifyS3ControlRequest("us-west-2", expectedHost);
    }

    @Test
    public void accessPointArn_fipsEnabled() {
        S3ControlClient s3Control =
            initializedBuilderForAccessPoint().region(Region.of("us-gov-east-1")).serviceConfiguration(b -> b.fipsModeEnabled(true)).build();

        String accessPointArn = "arn:aws-us-gov:s3:us-gov-east-1:123456789012:accesspoint:myendpoint";
        String expectedHost = "123456789012.s3-control.fips-us-gov-east-1.amazonaws.com";

        stubResponse();
        s3Control.getAccessPoint(b -> b.name(accessPointArn));

        verifyS3ControlRequest("us-gov-east-1", expectedHost);
    }

    @Test
    public void accessPointArn_fips2() {
        S3ControlClient s3Control = initializedBuilderForAccessPoint().region(Region.of("fips-us-gov-east-1")).build();

        String accessPointArn = "arn:aws-us-gov:s3:fips-us-gov-east-1:123456789012:accesspoint:myendpoint";
        String expectedHost = "123456789012.s3-control.fips-us-gov-east-1.amazonaws.com";

        stubResponse();
        s3Control.getAccessPoint(b -> b.name(accessPointArn));

        verifyS3ControlRequest("us-gov-east-1", expectedHost);
    }

    @Test
    public void clientFipsRegion_arnWithDifferentRegionUseArnRegionEnabled_shouldUseArnRegion() {
        S3ControlClient s3Control =
            initializedBuilderForAccessPoint().region(Region.of("fips-us-gov-east-1")).serviceConfiguration(b -> b.useArnRegionEnabled(true)).build();

        String accessPointArn = "arn:aws-us-gov:s3:us-gov-east-1:123456789012:accesspoint:myendpoint";
        String expectedHost = "123456789012.s3-control.us-gov-east-1.amazonaws.com";

        stubResponse();
        s3Control.getAccessPoint(b -> b.name(accessPointArn));

        verifyS3ControlRequest("us-gov-east-1", expectedHost);
    }

    @Test
    public void accessPointArnDifferentRegion_useArnRegionSet_shouldUseRegionFromArn() {
        S3ControlClient s3WithUseArnRegion =
            initializedBuilderForAccessPoint().region(Region.of("us-east-1")).serviceConfiguration(b -> b.useArnRegionEnabled(true)).build();

        String accessPointArn = "arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint";

        String expectedHost = "123456789012.s3-control.us-west-2.amazonaws.com";
        stubResponse();

        s3WithUseArnRegion.getAccessPoint(b -> b.name(accessPointArn));

        verifyS3ControlRequest("us-west-2", expectedHost);
    }

    private S3ControlClientBuilder initializedBuilderForAccessPoint() {
        return initializedBuilder();
    }

    @Override
    String expectedUrl() {
        return EXPECTED_URL;
    }
}
