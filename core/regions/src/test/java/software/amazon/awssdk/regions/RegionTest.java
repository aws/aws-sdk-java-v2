/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.regions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertSame;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

public class RegionTest {

    @Test(expected = NullPointerException.class)
    public void of_ThrowsNullPointerException_WhenNullValue() {
        Region.of(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void of_ThrowsIllegalArgumentException_WhenEmptyString() {
        Region.of("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void of_ThrowsIllegalArgumentException_WhenBlankString() {
        Region.of(" ");
    }

    @Test
    public void of_ReturnsRegion_WhenValidString() {
        Region region = Region.of("us-east-1");
        assertThat(region.id()).isEqualTo("us-east-1");
        assertSame(Region.US_EAST_1, region);
    }

    @Test
    public void sameValueSameClassAreSameInstance() {
        Region first = Region.of("first");
        Region alsoFirst = Region.of("first");

        assertThat(first).isSameAs(alsoFirst);
    }

    @Test
    public void canBeUsedAsKeysInMap() {
        Map<Region, String> someMap = new HashMap<>();
        someMap.put(Region.of("key"), "A Value");

        assertThat(someMap.get(Region.of("key"))).isEqualTo("A Value");
    }

    @Test
    public void regions_returnsAllRegions() {
        assertThat(Region.regions().stream().map(Region::id)
                .collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        "aws-global", "aws-cn-global", "ap-east-1",
                        "ap-northeast-1", "ap-northeast-2", "ap-southeast-1",
                        "ap-southeast-2", "ap-south-1", "cn-north-1",
                        "eu-central-1", "eu-north-1", "eu-west-1",
                        "eu-west-2", "eu-west-3", "us-gov-east-1",
                        "us-gov-west-1", "ca-central-1", "sa-east-1",
                        "us-east-1", "us-east-2", "us-west-1",
                        "us-west-2", "cn-northwest-1", "aws-us-gov-global"
                );
    }
}
