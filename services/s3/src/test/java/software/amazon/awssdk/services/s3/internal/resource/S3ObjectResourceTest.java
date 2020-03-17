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

public class S3ObjectResourceTest {
    @Test
    public void buildWithAllPropertiesSet() {
        S3ObjectResource s3ObjectResource = S3ObjectResource.builder()
                                                            .bucketName("bucket")
                                                            .key("key")
                                                            .accountId("account-id")
                                                            .partition("partition")
                                                            .region("region")
                                                            .build();

        assertEquals("bucket", s3ObjectResource.bucketName());
        assertEquals("key", s3ObjectResource.key());
        assertEquals(Optional.of("account-id"), s3ObjectResource.accountId());
        assertEquals(Optional.of("partition"), s3ObjectResource.partition());
        assertEquals(Optional.of("region"), s3ObjectResource.region());
        assertEquals("object", s3ObjectResource.type());
    }

    @Test
    public void toBuilder() {
        S3ObjectResource s3ObjectResource = S3ObjectResource.builder()
                                                            .bucketName("bucket")
                                                            .key("key")
                                                            .accountId("account-id")
                                                            .partition("partition")
                                                            .region("region")
                                                            .build()
                                                            .toBuilder()
                                                            .build();

        assertEquals("bucket", s3ObjectResource.bucketName());
        assertEquals("key", s3ObjectResource.key());
        assertEquals(Optional.of("account-id"), s3ObjectResource.accountId());
        assertEquals(Optional.of("partition"), s3ObjectResource.partition());
        assertEquals(Optional.of("region"), s3ObjectResource.region());
        assertEquals("object", s3ObjectResource.type());
    }

    @Test
    public void buildWithSetters() {
        S3ObjectResource.Builder builder = S3ObjectResource.builder();
        builder.setBucketName("bucket");
        builder.setKey("key");
        builder.setAccountId("account-id");
        builder.setPartition("partition");
        builder.setRegion("region");
        S3ObjectResource s3ObjectResource = builder.build();

        assertEquals("bucket", s3ObjectResource.bucketName());
        assertEquals("key", s3ObjectResource.key());
        assertEquals(Optional.of("account-id"), s3ObjectResource.accountId());
        assertEquals(Optional.of("partition"), s3ObjectResource.partition());
        assertEquals(Optional.of("region"), s3ObjectResource.region());
        assertEquals("object", s3ObjectResource.type());
    }

    @Test
    public void buildWithMinimalPropertiesSet() {
        S3ObjectResource s3ObjectResource = S3ObjectResource.builder()
                                                            .partition("aws")
                                                            .bucketName("bucket")
                                                            .key("key")
                                                            .build();

        assertEquals("bucket", s3ObjectResource.bucketName());
        assertEquals("key", s3ObjectResource.key());
        assertEquals(Optional.of("aws"), s3ObjectResource.partition());
        assertEquals(Optional.empty(), s3ObjectResource.accountId());
        assertEquals(Optional.empty(), s3ObjectResource.region());
        assertEquals("object", s3ObjectResource.type());
    }

    @Test(expected = NullPointerException.class)
    public void buildWithMissingPartition() {
        S3ObjectResource.builder()
                        .bucketName("bucket")
                        .key("key")
                        .build();
    }

    @Test(expected = NullPointerException.class)
    public void buildWithMissingBucketName() {
        S3ObjectResource.builder()
                        .partition("aws")
                        .key("key")
                        .build();
    }

    @Test(expected = NullPointerException.class)
    public void buildWithMissingKey() {
        S3ObjectResource.builder()
                        .partition("aws")
                        .bucketName("bucket-name")
                        .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithEmptyPartition() {
        S3ObjectResource.builder()
                        .bucketName("bucket")
                        .key("key")
                        .partition("")
                        .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithEmptyBucketName() {
        S3ObjectResource.builder()
                        .partition("aws")
                        .key("key")
                        .bucketName("")
                        .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithEmptyKey() {
        S3ObjectResource.builder()
                        .partition("aws")
                        .bucketName("bucket-name")
                        .key("")
                        .build();
    }

    @Test
    public void equals_allProperties() {
        S3ObjectResource s3ObjectResource1 = S3ObjectResource.builder()
                                                             .bucketName("bucket")
                                                             .key("key")
                                                             .accountId("account-id")
                                                             .partition("partition")
                                                             .region("region")
                                                             .build();

        S3ObjectResource s3ObjectResource2 = S3ObjectResource.builder()
                                                             .bucketName("bucket")
                                                             .key("key")
                                                             .accountId("account-id")
                                                             .partition("partition")
                                                             .region("region")
                                                             .build();

        S3ObjectResource s3ObjectResource3 = S3ObjectResource.builder()
                                                             .bucketName("bucket")
                                                             .key("key")
                                                             .accountId("account-id")
                                                             .partition("different-partition")
                                                             .region("region")
                                                             .build();

        assertEquals(s3ObjectResource1, s3ObjectResource2);
        assertNotEquals(s3ObjectResource1, s3ObjectResource3);
    }

    @Test
    public void equals_minimalProperties() {
        S3ObjectResource s3ObjectResource1 = S3ObjectResource.builder()
                                                             .partition("aws")
                                                             .bucketName("bucket")
                                                             .key("key")
                                                             .build();

        S3ObjectResource s3ObjectResource2 = S3ObjectResource.builder()
                                                             .partition("aws")
                                                             .bucketName("bucket")
                                                             .key("key")
                                                             .build();

        S3ObjectResource s3ObjectResource3 = S3ObjectResource.builder()
                                                             .partition("aws")
                                                             .bucketName("another-bucket")
                                                             .key("key")
                                                             .build();

        assertEquals(s3ObjectResource1, s3ObjectResource2);
        assertNotEquals(s3ObjectResource1, s3ObjectResource3);
    }

    @Test
    public void hashcode_allProperties() {
        S3ObjectResource s3ObjectResource1 = S3ObjectResource.builder()
                                                             .bucketName("bucket")
                                                             .key("key")
                                                             .accountId("account-id")
                                                             .partition("partition")
                                                             .region("region")
                                                             .build();

        S3ObjectResource s3ObjectResource2 = S3ObjectResource.builder()
                                                             .bucketName("bucket")
                                                             .key("key")
                                                             .accountId("account-id")
                                                             .partition("partition")
                                                             .region("region")
                                                             .build();

        S3ObjectResource s3ObjectResource3 = S3ObjectResource.builder()
                                                             .bucketName("bucket")
                                                             .key("key")
                                                             .accountId("account-id")
                                                             .partition("different-partition")
                                                             .region("region")
                                                             .build();

        assertEquals(s3ObjectResource1.hashCode(), s3ObjectResource2.hashCode());
        assertNotEquals(s3ObjectResource1.hashCode(), s3ObjectResource3.hashCode());
    }
}