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

public class S3ObjectResourceTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void buildWithBucketParent() {
        S3BucketResource parentResource = S3BucketResource.builder()
                                                          .bucketName("bucket")
                                                          .accountId("account-id")
                                                          .partition("partition")
                                                          .region("region")
                                                          .build();

        S3ObjectResource s3ObjectResource = S3ObjectResource.builder()
                                                            .key("key")
                                                            .parentS3Resource(parentResource)
                                                            .build();

        assertEquals("key", s3ObjectResource.key());
        assertEquals(Optional.of("account-id"), s3ObjectResource.accountId());
        assertEquals(Optional.of("partition"), s3ObjectResource.partition());
        assertEquals(Optional.of("region"), s3ObjectResource.region());
        assertEquals("object", s3ObjectResource.type());
        assertEquals(Optional.of(parentResource), s3ObjectResource.parentS3Resource());
    }

    @Test
    public void buildWithAccessPointParent() {
        S3AccessPointResource parentResource = S3AccessPointResource.builder()
                                                                    .accessPointName("test-ap")
                                                                    .accountId("account-id")
                                                                    .partition("partition")
                                                                    .region("region")
                                                                    .build();

        S3ObjectResource s3ObjectResource = S3ObjectResource.builder()
                                                            .key("key")
                                                            .parentS3Resource(parentResource)
                                                            .build();

        assertEquals("key", s3ObjectResource.key());
        assertEquals(Optional.of("account-id"), s3ObjectResource.accountId());
        assertEquals(Optional.of("partition"), s3ObjectResource.partition());
        assertEquals(Optional.of("region"), s3ObjectResource.region());
        assertEquals("object", s3ObjectResource.type());
        assertEquals(Optional.of(parentResource), s3ObjectResource.parentS3Resource());
    }

    @Test
    public void buildWithInvalidParentType() {
        S3Resource fakeS3ObjectResource = new S3Resource() {
            @Override
            public Optional<String> partition() {
                return Optional.empty();
            }

            @Override
            public Optional<String> region() {
                return Optional.empty();
            }

            @Override
            public Optional<String> accountId() {
                return Optional.empty();
            }

            @Override
            public String type() {
                return null;
            }
        };

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("parentS3Resource");
        S3ObjectResource.builder()
                        .parentS3Resource(fakeS3ObjectResource)
                        .key("key")
                        .build();
    }

    @Test
    public void buildWithMissingKey() {
        S3BucketResource parentResource = S3BucketResource.builder()
                                                          .bucketName("bucket")
                                                          .accountId("account-id")
                                                          .partition("partition")
                                                          .region("region")
                                                          .build();

        exception.expect(NullPointerException.class);
        exception.expectMessage("key");
        S3ObjectResource.builder()
                        .parentS3Resource(parentResource)
                        .build();
    }

    @Test
    public void buildWithMissingParent() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("parentS3Resource");
        S3ObjectResource.builder()
                        .key("test-key")
                        .build();
    }

    @Test
    public void equalsAndHashCode_allPropertiesSame() {
        S3BucketResource parentResource = S3BucketResource.builder()
                                                          .bucketName("bucket")
                                                          .accountId("account-id")
                                                          .partition("partition")
                                                          .region("region")
                                                          .build();
        S3ObjectResource s3ObjectResource1 = S3ObjectResource.builder()
                                                             .key("key")
                                                             .parentS3Resource(parentResource)
                                                             .build();

        S3ObjectResource s3ObjectResource2 = S3ObjectResource.builder()
                                                             .key("key")
                                                             .parentS3Resource(parentResource)
                                                             .build();

        assertEquals(s3ObjectResource1, s3ObjectResource2);
        assertEquals(s3ObjectResource1.hashCode(), s3ObjectResource2.hashCode());
    }

    @Test
    public void equalsAndHashCode_differentKey() {
        S3BucketResource parentResource = S3BucketResource.builder()
                                                          .bucketName("bucket")
                                                          .accountId("account-id")
                                                          .partition("partition")
                                                          .region("region")
                                                          .build();
        S3ObjectResource s3ObjectResource1 = S3ObjectResource.builder()
                                                             .key("key1")
                                                             .parentS3Resource(parentResource)
                                                             .build();

        S3ObjectResource s3ObjectResource2 = S3ObjectResource.builder()
                                                             .key("key2")
                                                             .parentS3Resource(parentResource)
                                                             .build();


        assertNotEquals(s3ObjectResource1, s3ObjectResource2);
        assertNotEquals(s3ObjectResource1.hashCode(), s3ObjectResource2.hashCode());
    }

    @Test
    public void equalsAndHashCode_differentParent() {
        S3BucketResource parentResource = S3BucketResource.builder()
                                                          .bucketName("bucket")
                                                          .accountId("account-id")
                                                          .partition("partition")
                                                          .region("region")
                                                          .build();

        S3BucketResource parentResource2 = S3BucketResource.builder()
                                                          .bucketName("bucket2")
                                                          .accountId("account-id")
                                                          .partition("partition")
                                                          .region("region")
                                                          .build();
        S3ObjectResource s3ObjectResource1 = S3ObjectResource.builder()
                                                             .key("key")
                                                             .parentS3Resource(parentResource)
                                                             .build();

        S3ObjectResource s3ObjectResource2 = S3ObjectResource.builder()
                                                             .key("key")
                                                             .parentS3Resource(parentResource2)
                                                             .build();


        assertNotEquals(s3ObjectResource1, s3ObjectResource2);
        assertNotEquals(s3ObjectResource1.hashCode(), s3ObjectResource2.hashCode());
    }

}