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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class S3AccessPointResourceTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void buildWithAllPropertiesSet() {
        S3AccessPointResource s3AccessPointResource = S3AccessPointResource.builder()
                                                                      .accessPointName("access_point-name")
                                                                      .accountId("account-id")
                                                                      .partition("partition")
                                                                      .region("region")
                                                                      .build();

        assertEquals("access_point-name", s3AccessPointResource.accessPointName());
        assertEquals(Optional.of("account-id"), s3AccessPointResource.accountId());
        assertEquals(Optional.of("partition"), s3AccessPointResource.partition());
        assertEquals(Optional.of("region"), s3AccessPointResource.region());
        assertEquals("accesspoint", s3AccessPointResource.type());
    }

    @Test
    public void toBuilder() {
        S3AccessPointResource s3AccessPointResource = S3AccessPointResource.builder()
                                                                      .accessPointName("access_point-name")
                                                                      .accountId("account-id")
                                                                      .partition("partition")
                                                                      .region("region")
                                                                      .build()
                                                                      .toBuilder()
                                                                      .build();

        assertEquals("access_point-name", s3AccessPointResource.accessPointName());
        assertEquals(Optional.of("account-id"), s3AccessPointResource.accountId());
        assertEquals(Optional.of("partition"), s3AccessPointResource.partition());
        assertEquals(Optional.of("region"), s3AccessPointResource.region());
        assertEquals("accesspoint", s3AccessPointResource.type());
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithBlankRegion() {
        S3AccessPointResource.builder()
                             .accessPointName("access_point-name")
                             .accountId("account-id")
                             .partition("partition")
                             .region("")
                             .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithBlankPartition() {
        S3AccessPointResource.builder()
                             .accessPointName("access_point-name")
                             .accountId("account-id")
                             .region("region")
                             .partition("")
                             .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithBlankAccountId() {
        S3AccessPointResource.builder()
                             .accessPointName("access_point-name")
                             .partition("partition")
                             .region("region")
                             .accountId("")
                             .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithBlankAccessPointName() {
        S3AccessPointResource.builder()
                             .accountId("account-id")
                             .partition("partition")
                             .region("region")
                             .accessPointName("")
                             .build();
    }

    @Test(expected = NullPointerException.class)
    public void buildWithMissingRegion() {
        S3AccessPointResource.builder()
                             .accessPointName("access_point-name")
                             .accountId("account-id")
                             .partition("partition")
                             .build();
    }

    @Test(expected = NullPointerException.class)
    public void buildWithMissingPartition() {
        S3AccessPointResource.builder()
                             .accessPointName("access_point-name")
                             .accountId("account-id")
                             .region("region")
                             .build();
    }

    @Test(expected = NullPointerException.class)
    public void buildWithMissingAccountId() {
        S3AccessPointResource.builder()
                             .accessPointName("access_point-name")
                             .partition("partition")
                             .region("region")
                             .build();
    }

    @Test(expected = NullPointerException.class)
    public void buildWithMissingAccessPointName() {
        S3AccessPointResource.builder()
                             .accountId("account-id")
                             .partition("partition")
                             .region("region")
                             .build();
    }

    @Test
    public void buildWithSetters() {
        S3AccessPointResource.Builder builder = S3AccessPointResource.builder();
        builder.setAccessPointName("access_point-name");
        builder.setAccountId("account-id");
        builder.setPartition("partition");
        builder.setRegion("region");
        S3AccessPointResource s3AccessPointResource = builder.build();

        assertEquals("access_point-name", s3AccessPointResource.accessPointName());
        assertEquals(Optional.of("account-id"), s3AccessPointResource.accountId());
        assertEquals(Optional.of("partition"), s3AccessPointResource.partition());
        assertEquals(Optional.of("region"), s3AccessPointResource.region());
        assertEquals("accesspoint", s3AccessPointResource.type());
    }

    @Test
    public void equalsHashcode_withoutParent() {
        S3AccessPointResource s3BucketResource1 = S3AccessPointResource.builder()
                                                                       .accessPointName("access_point")
                                                                       .accountId("account-id")
                                                                       .partition("partition")
                                                                       .region("region")
                                                                       .build();

        S3AccessPointResource s3BucketResource2 = S3AccessPointResource.builder()
                                                                       .accessPointName("access_point")
                                                                       .accountId("account-id")
                                                                       .partition("partition")
                                                                       .region("region")
                                                                       .build();

        S3AccessPointResource s3BucketResource3 = S3AccessPointResource.builder()
                                                                       .accessPointName("access_point")
                                                                       .accountId("account-id")
                                                                       .partition("different-partition")
                                                                       .region("region")
                                                                       .build();

        assertEquals(s3BucketResource1, s3BucketResource2);
        assertEquals(s3BucketResource1.hashCode(), s3BucketResource2.hashCode());
        assertNotEquals(s3BucketResource1, s3BucketResource3);
        assertNotEquals(s3BucketResource1.hashCode(), s3BucketResource3.hashCode());
    }

    @Test
    public void equalsHashcode_withParent() {
        S3OutpostResource parentResource = S3OutpostResource.builder()
                                                            .outpostId("1234")
                                                            .accountId("account-id")
                                                            .partition("partition")
                                                            .region("region")
                                                            .build();

        S3OutpostResource parentResource2 = S3OutpostResource.builder()
                                                             .outpostId("5678")
                                                             .accountId("account-id")
                                                             .partition("partition")
                                                             .region("region")
                                                             .build();

        S3AccessPointResource s3BucketResource1 = S3AccessPointResource.builder()
                                                                       .accessPointName("access_point")
                                                                       .parentS3Resource(parentResource)
                                                                       .build();

        S3AccessPointResource s3BucketResource2 = S3AccessPointResource.builder()
                                                                       .accessPointName("access_point")
                                                                       .parentS3Resource(parentResource)
                                                                       .build();

        S3AccessPointResource s3BucketResource3 = S3AccessPointResource.builder()
                                                                       .accessPointName("access_point")
                                                                       .parentS3Resource(parentResource2)
                                                                       .build();


        assertEquals(s3BucketResource1, s3BucketResource2);
        assertEquals(s3BucketResource1.hashCode(), s3BucketResource2.hashCode());
        assertNotEquals(s3BucketResource1, s3BucketResource3);
        assertNotEquals(s3BucketResource1.hashCode(), s3BucketResource3.hashCode());
    }

    @Test
    public void buildWithOutpostParent() {
        S3OutpostResource parentResource = S3OutpostResource.builder()
                                                            .outpostId("1234")
                                                            .accountId("account-id")
                                                            .partition("partition")
                                                            .region("region")
                                                            .build();
        S3AccessPointResource s3AccessPointResource = S3AccessPointResource.builder()
                                                                         .parentS3Resource(parentResource)
                                                                         .accessPointName("access-point-name")
                                                                         .build();

        assertEquals("access-point-name", s3AccessPointResource.accessPointName());
        assertEquals(Optional.of("account-id"), s3AccessPointResource.accountId());
        assertEquals(Optional.of("partition"), s3AccessPointResource.partition());
        assertEquals(Optional.of("region"), s3AccessPointResource.region());
        assertEquals("accesspoint", s3AccessPointResource.type());
        assertEquals(Optional.of(parentResource), s3AccessPointResource.parentS3Resource());
    }

    @Test
    public void buildWithInvalidParent_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("parentS3Resource");

        S3BucketResource invalidParent = S3BucketResource.builder()
                                                         .bucketName("bucket")
                                                         .build();
        S3AccessPointResource.builder()
                             .parentS3Resource(invalidParent)
                             .accessPointName("access-point-name")
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

        S3AccessPointResource.builder()
                             .accessPointName("access_point")
                             .partition("partition")
                             .parentS3Resource(parentResource)
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

        S3AccessPointResource.builder()
                             .accessPointName("access_point")
                             .accountId("account id")
                             .parentS3Resource(parentResource)
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

        S3AccessPointResource.builder()
                             .accessPointName("access_point")
                             .region("region")
                             .parentS3Resource(parentResource)
                             .build();
    }
}