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
package software.amazon.awssdk.codegen.lite.regions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RegionValidationUtilTest {

    private static final String AWS_PARTITION_REGEX = "^(us|eu|ap|sa|ca)\\-\\w+\\-\\d+$";
    private static final String AWS_CN_PARTITION_REGEX = "^cn\\-\\w+\\-\\d+$";
    private static final String AWS_GOV_PARTITION_REGEX = "^us\\-gov\\-\\w+\\-\\d+$";

    @Test
    public void usEast1_AwsPartition_IsValidRegion() {
        assertTrue(RegionValidationUtil.validRegion("us-east-1", AWS_PARTITION_REGEX));
    }

    @Test
    public void usWest2Fips_AwsPartition_IsValidRegion() {
        assertTrue(RegionValidationUtil.validRegion("us-west-2-fips", AWS_PARTITION_REGEX));
    }

    @Test
    public void fipsUsWest2_AwsPartition_IsNotValidRegion() {
        assertTrue(RegionValidationUtil.validRegion("fips-us-west-2", AWS_PARTITION_REGEX));
    }

    @Test
    public void fips_AwsPartition_IsNotValidRegion() {
        assertFalse(RegionValidationUtil.validRegion("fips", AWS_PARTITION_REGEX));
    }

    @Test
    public void prodFips_AwsPartition_IsNotValidRegion() {
        assertFalse(RegionValidationUtil.validRegion("ProdFips", AWS_PARTITION_REGEX));
    }

    @Test
    public void cnNorth1_AwsCnPartition_IsNotValidRegion() {
        assertFalse(RegionValidationUtil.validRegion("cn-north-1", AWS_PARTITION_REGEX));
    }

    @Test
    public void cnNorth1_AwsCnPartition_IsValidRegion() {
        assertTrue(RegionValidationUtil.validRegion("cn-north-1", AWS_CN_PARTITION_REGEX));
    }

    @Test
    public void usEast1_AwsCnPartition_IsNotValidRegion() {
        assertFalse(RegionValidationUtil.validRegion("us-east-1", AWS_CN_PARTITION_REGEX));
    }

    @Test
    public void usGovWest1_AwsGovPartition_IsValidRegion() {
        assertTrue(RegionValidationUtil.validRegion("us-gov-west-1", AWS_GOV_PARTITION_REGEX));
    }

    @Test
    public void usGovWest1Fips_AwsGovPartition_IsValidRegion() {
        assertTrue(RegionValidationUtil.validRegion("us-gov-west-1-fips", AWS_GOV_PARTITION_REGEX));
    }

    @Test
    public void fipsUsGovWest1_AwsGovPartition_IsNotValidRegion() {
        assertTrue(RegionValidationUtil.validRegion("fips-us-gov-west-1", AWS_GOV_PARTITION_REGEX));
    }

    @Test
    public void fips_AwsGovPartition_IsNotValidRegion() {
        assertFalse(RegionValidationUtil.validRegion("fips", AWS_GOV_PARTITION_REGEX));
    }

    @Test
    public void prodFips_AwsGovPartition_IsNotValidRegion() {
        assertFalse(RegionValidationUtil.validRegion("ProdFips", AWS_GOV_PARTITION_REGEX));
    }

    @Test
    public void cnNorth1_AwsGovPartition_IsNotValidRegion() {
        assertFalse(RegionValidationUtil.validRegion("cn-north-1", AWS_GOV_PARTITION_REGEX));
    }

    @Test
    public void awsGlobal_AwsPartition_IsValidRegion() {
        assertTrue(RegionValidationUtil.validRegion("aws-global", AWS_PARTITION_REGEX));
    }

    @Test
    public void awsGovGlobal_AwsGovPartition_IsValidRegion() {
        assertTrue(RegionValidationUtil.validRegion("aws-us-gov-global", AWS_GOV_PARTITION_REGEX));
    }

    @Test
    public void awsCnGlobal_AwsCnPartition_IsValidRegion() {
        assertTrue(RegionValidationUtil.validRegion("aws-cn-global", AWS_CN_PARTITION_REGEX));
    }
}
