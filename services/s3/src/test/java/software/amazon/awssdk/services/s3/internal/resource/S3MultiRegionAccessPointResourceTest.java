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

package software.amazon.awssdk.services.s3.internal.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.arns.Arn;

public class S3MultiRegionAccessPointResourceTest {

    public static String VALID_MRAP_ARN = "arn:aws:s3::123456789012:accesspoint:mfzwi23gnjvgw.mrap";
    public static String INVALID_MRAP_ARN_WITH_REGION = "arn:aws:s3:us-west-2:123456789012:accesspoint:mfzwi23gnjvgw.mrap";
    public static String INVALID_MRAP_ARN_WITHOUT_SUFFIX = "arn:aws:s3::123456789012:accesspoint:mfzwi23gnjvgw";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void isMultiRegion_ArnContainsRegion_shouldReturnFalse() {
        assertThat(S3MultiRegionAccessPointResource.isMultiRegion(Arn.fromString(INVALID_MRAP_ARN_WITH_REGION))).isFalse();
    }

    @Test
    public void isMultiRegion_ArnDoesNotEndWithMrap_shouldReturnFalse() {
        assertThat(S3MultiRegionAccessPointResource.isMultiRegion(Arn.fromString(INVALID_MRAP_ARN_WITHOUT_SUFFIX))).isFalse();
    }

    @Test
    public void isMultiRegion_CorrectMrapFormat_shouldReturnTrue() {
        assertThat(S3MultiRegionAccessPointResource.isMultiRegion(Arn.fromString(VALID_MRAP_ARN))).isTrue();
    }

}