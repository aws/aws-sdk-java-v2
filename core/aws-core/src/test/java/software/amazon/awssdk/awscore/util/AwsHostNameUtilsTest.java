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

package software.amazon.awssdk.awscore.util;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.awscore.util.AwsHostNameUtils.parseSigningRegion;

import org.junit.Test;
import software.amazon.awssdk.regions.Region;

/** Unit tests for the utility methods that parse information from AWS URLs. */
public class AwsHostNameUtilsTest {
    @Test
    public void testStandardNoHint() {
        // Verify that standard endpoints parse correctly without a service hint
        assertThat(parseSigningRegion("iam.amazonaws.com", null)).hasValue(Region.US_EAST_1);
        assertThat(parseSigningRegion("iam.us-west-2.amazonaws.com", null)).hasValue(Region.US_WEST_2);
        assertThat(parseSigningRegion("ec2.us-west-2.amazonaws.com", null)).hasValue(Region.US_WEST_2);

        assertThat(parseSigningRegion("cloudsearch.us-west-2.amazonaws.com", null)).hasValue(Region.US_WEST_2);
        assertThat(parseSigningRegion("domain.us-west-2.cloudsearch.amazonaws.com", null)).hasValue(Region.US_WEST_2);
    }

    @Test
    public void testStandard() {
        // Verify that standard endpoints parse correctly with a service hint
        assertThat(parseSigningRegion("iam.amazonaws.com", "iam")).hasValue(Region.US_EAST_1);
        assertThat(parseSigningRegion("iam.us-west-2.amazonaws.com", "iam")).hasValue(Region.US_WEST_2);
        assertThat(parseSigningRegion("ec2.us-west-2.amazonaws.com", "ec2")).hasValue(Region.US_WEST_2);

        assertThat(parseSigningRegion("cloudsearch.us-west-2.amazonaws.com", "cloudsearch")).hasValue(Region.US_WEST_2);
        assertThat(parseSigningRegion("domain.us-west-2.cloudsearch.amazonaws.com", "cloudsearch")).hasValue(Region.US_WEST_2);
    }

    @Test
    public void testBjs() {
        // Verify that BJS endpoints parse correctly even though they're non-standard.
        assertThat(parseSigningRegion("iam.cn-north-1.amazonaws.com.cn", "iam")).hasValue(Region.CN_NORTH_1);
        assertThat(parseSigningRegion("ec2.cn-north-1.amazonaws.com.cn", "ec2")).hasValue(Region.CN_NORTH_1);
        assertThat(parseSigningRegion("s3.cn-north-1.amazonaws.com.cn", "s3")).hasValue(Region.CN_NORTH_1);
        assertThat(parseSigningRegion("bucket.name.with.periods.s3.cn-north-1.amazonaws.com.cn", "s3")).hasValue(Region.CN_NORTH_1);

        assertThat(parseSigningRegion("cloudsearch.cn-north-1.amazonaws.com.cn", "cloudsearch")).hasValue(Region.CN_NORTH_1);
        assertThat(parseSigningRegion("domain.cn-north-1.cloudsearch.amazonaws.com.cn", "cloudsearch")).hasValue(Region.CN_NORTH_1);
    }

    @Test
    public void testParseRegionWithIpv4() {
        assertThat(parseSigningRegion("54.231.16.200", null)).isNotPresent();
    }
}
