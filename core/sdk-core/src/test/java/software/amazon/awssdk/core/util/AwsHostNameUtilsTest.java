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

package software.amazon.awssdk.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static software.amazon.awssdk.core.util.AwsHostNameUtils.parseRegion;
import static software.amazon.awssdk.core.util.AwsHostNameUtils.parseRegionName;
import static software.amazon.awssdk.core.util.AwsHostNameUtils.parseServiceName;

import java.net.URI;
import org.junit.Test;

/** Unit tests for the utility methods that parse information from AWS URLs. */
public class AwsHostNameUtilsTest {

    private static final URI IAM_ENDPOINT = URI.create("https://iam.amazonaws.com");
    private static final URI IAM_REGION_ENDPOINT = URI.create("https://iam.us-west-2.amazonaws.com");
    private static final URI EC2_REGION_ENDPOINT = URI.create("https://ec2.us-west-2.amazonaws.com");
    private static final URI S3_ENDPOINT = URI.create("https://s3.amazonaws.com");
    private static final URI S3_BUCKET_ENDPOINT = URI.create("https://bucket.name.with.periods.s3.amazonaws.com");
    private static final URI S3_REGION_ENDPOINT = URI.create("https://s3-eu-west-1.amazonaws.com");
    private static final URI S3_BUCKET_REGION_ENDPOINT = URI.create("https://bucket.name.with.periods.s3-eu-west-1.amazonaws.com");

    @Test
    public void testParseServiceName() {
        // Verify that parseServiceName keeps working the way it used to.
        assertEquals("iam", parseServiceName(IAM_ENDPOINT));
        assertEquals("iam", parseServiceName(IAM_REGION_ENDPOINT));
        assertEquals("ec2", parseServiceName(EC2_REGION_ENDPOINT));
        assertEquals("s3", parseServiceName(S3_ENDPOINT));
        assertEquals("s3", parseServiceName(S3_BUCKET_ENDPOINT));
        assertEquals("s3", parseServiceName(S3_REGION_ENDPOINT));
        assertEquals("s3", parseServiceName(S3_BUCKET_REGION_ENDPOINT));
    }

    @Test
    public void testStandardNoHint() {
        // Verify that standard endpoints parse correctly without a service hint
        assertEquals("us-east-1", parseRegionName("iam.amazonaws.com", null));
        assertEquals("us-west-2", parseRegionName("iam.us-west-2.amazonaws.com", null));
        assertEquals("us-west-2", parseRegionName("ec2.us-west-2.amazonaws.com", null));

        assertEquals("us-west-2", parseRegionName("cloudsearch.us-west-2.amazonaws.com", null));
        assertEquals("us-west-2", parseRegionName("domain.us-west-2.cloudsearch.amazonaws.com", null));
    }

    @Test
    public void testStandard() {
        // Verify that standard endpoints parse correctly with a service hint
        assertEquals("us-east-1", parseRegionName("iam.amazonaws.com", "iam"));
        assertEquals("us-west-2", parseRegionName("iam.us-west-2.amazonaws.com", "iam"));
        assertEquals("us-west-2", parseRegionName("ec2.us-west-2.amazonaws.com", "ec2"));

        assertEquals("us-west-2", parseRegionName("cloudsearch.us-west-2.amazonaws.com", "cloudsearch"));
        assertEquals("us-west-2", parseRegionName("domain.us-west-2.cloudsearch.amazonaws.com", "cloudsearch"));
    }

    @Test
    public void testBjs() {
        // Verify that BJS endpoints parse correctly even though they're non-standard.
        assertEquals("cn-north-1", parseRegionName("iam.cn-north-1.amazonaws.com.cn", "iam"));
        assertEquals("cn-north-1", parseRegionName("ec2.cn-north-1.amazonaws.com.cn", "ec2"));
        assertEquals("cn-north-1", parseRegionName("s3.cn-north-1.amazonaws.com.cn", "s3"));
        assertEquals("cn-north-1", parseRegionName("bucket.name.with.periods.s3.cn-north-1.amazonaws.com.cn", "s3"));

        assertEquals("cn-north-1", parseRegionName("cloudsearch.cn-north-1.amazonaws.com.cn", "cloudsearch"));
        assertEquals("cn-north-1", parseRegionName("domain.cn-north-1.cloudsearch.amazonaws.com.cn", "cloudsearch"));
    }

    @Test
    public void testParseRegionWithStandardEndpointsNoHint() {
        // Verify that standard endpoints parse correctly without a service hint
        assertEquals("us-east-1", parseRegion("iam.amazonaws.com", null));
        assertEquals("us-west-2", parseRegion("iam.us-west-2.amazonaws.com", null));
        assertEquals("us-west-2", parseRegion("ec2.us-west-2.amazonaws.com", null));

        assertEquals("us-west-2", parseRegion("cloudsearch.us-west-2.amazonaws.com", null));
        assertEquals("us-west-2", parseRegion("domain.us-west-2.cloudsearch.amazonaws.com", null));
    }

    @Test
    public void testParseRegionWithStandardEndpointsWithServiceHint() {
        // Verify that standard endpoints parse correctly with a service hint
        assertEquals("us-east-1", parseRegion("iam.amazonaws.com", "iam"));
        assertEquals("us-west-2", parseRegion("iam.us-west-2.amazonaws.com", "iam"));
        assertEquals("us-west-2", parseRegion("ec2.us-west-2.amazonaws.com", "ec2"));

        assertEquals("us-west-2", parseRegion("cloudsearch.us-west-2.amazonaws.com", "cloudsearch"));
        assertEquals("us-west-2", parseRegion("domain.us-west-2.cloudsearch.amazonaws.com", "cloudsearch"));
    }

    @Test
    public void testParseRegionWithBjsEndpoints() {
        // Verify that BJS endpoints parse correctly even though they're non-standard.
        assertEquals("cn-north-1", parseRegion("iam.cn-north-1.amazonaws.com.cn", "iam"));
        assertEquals("cn-north-1", parseRegion("ec2.cn-north-1.amazonaws.com.cn", "ec2"));

        assertEquals("cn-north-1", parseRegion("cloudsearch.cn-north-1.amazonaws.com.cn", "cloudsearch"));
        assertEquals("cn-north-1", parseRegion("domain.cn-north-1.cloudsearch.amazonaws.com.cn", "cloudsearch"));
    }

    @Test
    public void testParseRegionWithIpv4() {
        assertNull(parseRegion("54.231.16.200", null));
    }
}
