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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Optional;

import org.junit.Test;

public class S3BucketResourceTest {
    @Test
    public void buildWithAllPropertiesSet() {
        S3BucketResource s3BucketResource = S3BucketResource.builder()
                                                            .bucketName("bucket")
                                                            .accountId("account-id")
                                                            .partition("partition")
                                                            .region("region")
                                                            .build();

        assertEquals("bucket", s3BucketResource.bucketName());
        assertEquals(Optional.of("account-id"), s3BucketResource.accountId());
        assertEquals(Optional.of("partition"), s3BucketResource.partition());
        assertEquals(Optional.of("region"), s3BucketResource.region());
        assertEquals("bucket_name", s3BucketResource.type());
    }

    @Test
    public void toBuilder() {
        S3BucketResource s3BucketResource = S3BucketResource.builder()
                                                            .bucketName("bucket")
                                                            .accountId("account-id")
                                                            .partition("partition")
                                                            .region("region")
                                                            .build()
                                                            .toBuilder()
                                                            .build();

        assertEquals("bucket", s3BucketResource.bucketName());
        assertEquals(Optional.of("account-id"), s3BucketResource.accountId());
        assertEquals(Optional.of("partition"), s3BucketResource.partition());
        assertEquals(Optional.of("region"), s3BucketResource.region());
        assertEquals("bucket_name", s3BucketResource.type());
    }

    @Test
    public void buildWithSetters() {
        S3BucketResource.Builder builder = S3BucketResource.builder();
        builder.setBucketName("bucket");
        builder.setAccountId("account-id");
        builder.setPartition("partition");
        builder.setRegion("region");
        S3BucketResource s3BucketResource = builder.build();

        assertEquals("bucket", s3BucketResource.bucketName());
        assertEquals(Optional.of("account-id"), s3BucketResource.accountId());
        assertEquals(Optional.of("partition"), s3BucketResource.partition());
        assertEquals(Optional.of("region"), s3BucketResource.region());
        assertEquals("bucket_name", s3BucketResource.type());
    }

    @Test
    public void buildWithMinimalPropertiesSet() {
        S3BucketResource s3BucketResource = S3BucketResource.builder()
                                                            .bucketName("bucket")
                                                            .build();

        assertEquals("bucket", s3BucketResource.bucketName());
        assertEquals(Optional.empty(), s3BucketResource.accountId());
        assertEquals(Optional.empty(), s3BucketResource.partition());
        assertEquals(Optional.empty(), s3BucketResource.region());
        assertEquals("bucket_name", s3BucketResource.type());
    }

    @Test(expected = NullPointerException.class)
    public void buildWithMissingBucketName() {
        S3BucketResource.builder().build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithBlankBucketName() {
        S3BucketResource.builder().bucketName("").build();
    }

    @Test
    public void equals_allProperties() {
        S3BucketResource s3BucketResource1 = S3BucketResource.builder()
                                                            .bucketName("bucket")
                                                            .accountId("account-id")
                                                            .partition("partition")
                                                            .region("region")
                                                            .build();

        S3BucketResource s3BucketResource2 = S3BucketResource.builder()
                                                             .bucketName("bucket")
                                                             .accountId("account-id")
                                                             .partition("partition")
                                                             .region("region")
                                                             .build();

        S3BucketResource s3BucketResource3 = S3BucketResource.builder()
                                                             .bucketName("bucket")
                                                             .accountId("account-id")
                                                             .partition("different-partition")
                                                             .region("region")
                                                             .build();

        assertEquals(s3BucketResource1, s3BucketResource2);
        assertNotEquals(s3BucketResource1, s3BucketResource3);
    }

    @Test
    public void equals_minimalProperties() {
        S3BucketResource s3BucketResource1 = S3BucketResource.builder()
                                                             .bucketName("bucket")
                                                             .build();

        S3BucketResource s3BucketResource2 = S3BucketResource.builder()
                                                             .bucketName("bucket")
                                                             .build();

        S3BucketResource s3BucketResource3 = S3BucketResource.builder()
                                                             .bucketName("another-bucket")
                                                             .build();

        assertEquals(s3BucketResource1, s3BucketResource2);
        assertNotEquals(s3BucketResource1, s3BucketResource3);
    }

    @Test
    public void hashcode_allProperties() {
        S3BucketResource s3BucketResource1 = S3BucketResource.builder()
                                                             .bucketName("bucket")
                                                             .accountId("account-id")
                                                             .partition("partition")
                                                             .region("region")
                                                             .build();

        S3BucketResource s3BucketResource2 = S3BucketResource.builder()
                                                             .bucketName("bucket")
                                                             .accountId("account-id")
                                                             .partition("partition")
                                                             .region("region")
                                                             .build();

        S3BucketResource s3BucketResource3 = S3BucketResource.builder()
                                                             .bucketName("bucket")
                                                             .accountId("account-id")
                                                             .partition("different-partition")
                                                             .region("region")
                                                             .build();

        assertEquals(s3BucketResource1.hashCode(), s3BucketResource2.hashCode());
        assertNotEquals(s3BucketResource1.hashCode(), s3BucketResource3.hashCode());
    }
}