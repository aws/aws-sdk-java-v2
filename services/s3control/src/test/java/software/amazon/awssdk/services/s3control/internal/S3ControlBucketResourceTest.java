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
package software.amazon.awssdk.services.s3control.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.services.s3.internal.resource.S3BucketResource;
import software.amazon.awssdk.services.s3.internal.resource.S3OutpostResource;
import software.amazon.awssdk.services.s3control.S3ControlBucketResource;

public class S3ControlBucketResourceTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void buildWithAllPropertiesSet() {
        S3ControlBucketResource bucketResource = S3ControlBucketResource.builder()
                                                                        .bucketName("bucket")
                                                                        .accountId("account-id")
                                                                        .partition("partition")
                                                                        .region("region")
                                                                        .build();

        assertEquals("bucket", bucketResource.bucketName());
        assertEquals(Optional.of("account-id"), bucketResource.accountId());
        assertEquals(Optional.of("partition"), bucketResource.partition());
        assertEquals(Optional.of("region"), bucketResource.region());
        assertEquals(S3ControlResourceType.BUCKET.toString(), bucketResource.type());
    }

    @Test(expected = NullPointerException.class)
    public void buildWithMissingBucketName() {
        S3ControlBucketResource.builder().build();
    }

    @Test
    public void equals_allProperties() {
        S3OutpostResource parentResource = S3OutpostResource.builder()
                                                            .outpostId("1234")
                                                            .accountId("account-id")
                                                            .partition("partition")
                                                            .region("region")
                                                            .build();
        S3ControlBucketResource bucketResource1 = S3ControlBucketResource.builder()
                                                                         .bucketName("bucket")
                                                                         .parentS3Resource(parentResource)
                                                                         .build();

        S3ControlBucketResource bucketResource2 = S3ControlBucketResource.builder()
                                                                         .bucketName("bucket")
                                                                         .parentS3Resource(parentResource)
                                                                         .build();

        S3ControlBucketResource bucketResource3 = S3ControlBucketResource.builder()
                                                                         .bucketName("bucket")
                                                                         .accountId("account-id")
                                                                         .partition("different-partition")
                                                                         .region("region")
                                                                         .build();

        assertEquals(bucketResource1, bucketResource2);
        assertNotEquals(bucketResource1, bucketResource3);
    }

    @Test
    public void hashcode_allProperties() {
        S3OutpostResource parentResource = S3OutpostResource.builder()
                                                            .outpostId("1234")
                                                            .accountId("account-id")
                                                            .partition("partition")
                                                            .region("region")
                                                            .build();
        S3ControlBucketResource bucketResource1 = S3ControlBucketResource.builder()
                                                                         .bucketName("bucket")
                                                                         .parentS3Resource(parentResource)
                                                                         .build();

        S3ControlBucketResource bucketResource2 = S3ControlBucketResource.builder()
                                                                         .bucketName("bucket")
                                                                         .parentS3Resource(parentResource)
                                                                         .build();

        S3ControlBucketResource bucketResource3 = S3ControlBucketResource.builder()
                                                                         .bucketName("bucket")
                                                                         .accountId("account-id")
                                                                         .partition("different-partition")
                                                                         .region("region")
                                                                         .build();

        assertEquals(bucketResource1.hashCode(), bucketResource2.hashCode());
        assertNotEquals(bucketResource1.hashCode(), bucketResource3.hashCode());
    }

    @Test
    public void buildWithOutpostParent() {
        S3OutpostResource parentResource = S3OutpostResource.builder()
                                                            .outpostId("1234")
                                                            .accountId("account-id")
                                                            .partition("partition")
                                                            .region("region")
                                                            .build();

        S3ControlBucketResource bucketResource = S3ControlBucketResource.builder()
                                                                        .bucketName("bucket")
                                                                        .parentS3Resource(parentResource)
                                                                        .build();

        assertEquals(Optional.of("account-id"), bucketResource.accountId());
        assertEquals(Optional.of("partition"), bucketResource.partition());
        assertEquals(Optional.of("region"), bucketResource.region());
        assertEquals("bucket", bucketResource.bucketName());
        assertEquals("bucket", bucketResource.type());
        assertEquals(Optional.of(parentResource), bucketResource.parentS3Resource());
    }

    @Test
    public void buildWithInvalidParent_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("parentS3Resource");

        S3BucketResource invalidParent = S3BucketResource.builder()
                                                         .bucketName("bucket")
                                                         .build();
        S3ControlBucketResource.builder()
                               .parentS3Resource(invalidParent)
                               .bucketName("bucketName")
                               .build();
    }

    @Test
    public void hasParentAndRegion_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("has parent resource");

        S3OutpostResource parentResource = S3OutpostResource.builder()
                                                            .outpostId("1234")
                                                            .accountId("account-id")
                                                            .partition("partition")
                                                            .region("region")
                                                            .build();
        S3ControlBucketResource.builder()
                               .parentS3Resource(parentResource)
                               .region("us-east-1")
                               .bucketName("bucketName")
                               .build();
    }

    @Test
    public void hasParentAndPartition_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("has parent resource");

        S3OutpostResource parentResource = S3OutpostResource.builder()
                                                            .outpostId("1234")
                                                            .accountId("account-id")
                                                            .partition("partition")
                                                            .region("region")
                                                            .build();
        S3ControlBucketResource.builder()
                               .parentS3Resource(parentResource)
                               .partition("partition")
                               .bucketName("bucketName")
                               .build();
    }

    @Test
    public void hasParentAndAccountId_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("has parent resource");

        S3OutpostResource parentResource = S3OutpostResource.builder()
                                                            .outpostId("1234")
                                                            .accountId("account-id")
                                                            .partition("partition")
                                                            .region("region")
                                                            .build();
        S3ControlBucketResource.builder()
                               .parentS3Resource(parentResource)
                               .accountId("account-id")
                               .bucketName("bucketName")
                               .build();
    }
}