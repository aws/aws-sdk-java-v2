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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;

/**
 * Integration tests for object tagging support.
 */
public class ObjectTaggingIntegrationTest extends S3IntegrationTestBase {
    private static final String KEY_PREFIX = "tagged-object-";
    private static final String BUCKET = temporaryBucketName("java-object-tagging-bucket-");

    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();
        createBucket(BUCKET);

        s3.putBucketVersioning(PutBucketVersioningRequest.builder()
                                                         .bucket(BUCKET)
                                                         .versioningConfiguration(
                                                                 VersioningConfiguration.builder()
                                                                                        .status(BucketVersioningStatus.ENABLED)
                                                                                        .build())
                                                         .build());
    }

    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    public void putObject_WithTagging_Succeeds() {
        Tagging tags = Tagging.builder()
                              .tagSet(Tag.builder()
                                         .key("foo")
                                         .value("1").build(), Tag.builder()
                                         .key("bar")
                                         .value("2").build(), Tag.builder()
                                         .key("baz")
                                         .value("3").build())
                              .build();

        String key = makeNewKey();
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(key)
                                     .tagging(tags)
                                     .build(),
                     RequestBody.empty());

        GetObjectTaggingResponse response = s3.getObjectTagging(GetObjectTaggingRequest.builder()
                                                                                       .bucket(BUCKET)
                                                                                       .key(key)
                                                                                       .build());

        assertThat(tags.tagSet().size()).isEqualTo(s3.getObjectTagging(GetObjectTaggingRequest.builder()
                                                                                              .bucket(BUCKET)
                                                                                              .key(key)
                                                                                              .build())
                                                     .tagSet().size());
    }

    @Test
    public void getObjectTagging_Succeeds() {
        List<Tag> tagSet = new ArrayList<>();
        tagSet.add(Tag.builder().key("foo").value("1").build());
        tagSet.add(Tag.builder().key("bar").value("2").build());
        tagSet.add(Tag.builder().key("baz").value("3").build());

        Tagging tags = Tagging.builder().tagSet(tagSet).build();

        String key = makeNewKey();
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(key)
                                     .tagging(tags)
                                     .build(), RequestBody.empty());

        List<Tag> getTaggingResult = s3.getObjectTagging(GetObjectTaggingRequest.builder()
                                                                                .bucket(BUCKET)
                                                                                .key(key)
                                                                                .build())
                                       .tagSet();


        assertThat(getTaggingResult).containsExactlyInAnyOrder(tags.tagSet().toArray(new Tag[tags.tagSet().size()]));
    }

    @Test
    public void putObjectTagging_Succeeds_WithUrlEncodedTags() {
        List<Tag> tagSet = new ArrayList<>();
        tagSet.add(Tag.builder().key("foo").value("bar @baz").build());
        tagSet.add(Tag.builder().key("foo bar").value("baz").build());
        tagSet.add(Tag.builder().key("foo/bar").value("baz").build());

        Tagging tags = Tagging.builder().tagSet(tagSet).build();

        String key = makeNewKey();
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(key)
                                     .tagging(tags)
                                     .build(), RequestBody.empty());

        List<Tag> getTaggingResult = s3.getObjectTagging(GetObjectTaggingRequest.builder()
                                                                                .bucket(BUCKET)
                                                                                .key(key)
                                                                                .build())
                                       .tagSet();


        assertThat(getTaggingResult).containsExactlyInAnyOrder(tags.tagSet().toArray(new Tag[tags.tagSet().size()]));
    }

    @Test
    public void copyObject_Succeeds_WithNewTags() {
        List<Tag> tagSet = new ArrayList<>();
        tagSet.add(Tag.builder().key("foo").value("1").build());
        tagSet.add(Tag.builder().key("bar").value("2").build());
        tagSet.add(Tag.builder().key("baz").value("3").build());

        Tagging tags = Tagging.builder().tagSet(tagSet).build();

        String key = makeNewKey();
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(key)
                                     .tagging(tags)
                                     .build(), RequestBody.empty());

        String destKey = makeNewKey();
        List<Tag> tagSet2 = new ArrayList<>();
        tagSet2.add(Tag.builder().key("foo1").value("1").build());
        tagSet2.add(Tag.builder().key("bar2").value("2").build());
        Tagging tagsCopy = Tagging.builder().tagSet(tagSet).build();

        s3.copyObject(CopyObjectRequest.builder()
                                       .copySource(BUCKET + "/" + key)
                                       .bucket(BUCKET)
                                       .key(destKey)
                                       .tagging(tagsCopy)
                                       .build());

        List<Tag> getTaggingResult = s3.getObjectTagging(GetObjectTaggingRequest.builder()
                                                                                .bucket(BUCKET)
                                                                                .key(key)
                                                                                .build())
                                       .tagSet();

        assertThat(getTaggingResult).containsExactlyInAnyOrder(tagsCopy.tagSet().toArray(new Tag[tagsCopy.tagSet().size()]));
    }

    @Test
    public void testDeleteObjectTagging() {
        List<Tag> tagSet = new ArrayList<>();
        tagSet.add(Tag.builder().key("foo").value("1").build());
        tagSet.add(Tag.builder().key("bar").value("2").build());
        tagSet.add(Tag.builder().key("baz").value("3").build());

        Tagging tags = Tagging.builder().tagSet(tagSet).build();

        String key = makeNewKey();
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(key)
                                     .tagging(tags)
                                     .build(), RequestBody.empty());

        s3.deleteObjectTagging(DeleteObjectTaggingRequest.builder().bucket(BUCKET).key(key).build());

        List<Tag> getTaggingResult = s3.getObjectTagging(GetObjectTaggingRequest.builder()
                                                                                .bucket(BUCKET)
                                                                                .key(key)
                                                                                .build())
                                       .tagSet();

        assertThat(getTaggingResult.size()).isEqualTo(0);
    }

    private String makeNewKey() {
        return KEY_PREFIX + System.currentTimeMillis();
    }
}
