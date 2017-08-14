/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import software.amazon.awssdk.util.json.Jackson;

public class S3LinkIdTest {

    @Test
    public void testToFromJson() {
        String region = "ap-northeast-1";
        S3Link.Id id = new S3Link.Id(region, "bucket", "key");
        String json = id.toJson();
        S3Link.Id twin = Jackson.fromJsonString(json, S3Link.Id.class);
        assertEquals("bucket", twin.bucket());
        assertEquals("key", twin.getKey());
        assertEquals(region, twin.getRegionId());
    }
}
