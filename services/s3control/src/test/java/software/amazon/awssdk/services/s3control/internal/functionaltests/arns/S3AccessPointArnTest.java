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

    @Override
    String expectedUrl() {
        return EXPECTED_URL;
    }
}
