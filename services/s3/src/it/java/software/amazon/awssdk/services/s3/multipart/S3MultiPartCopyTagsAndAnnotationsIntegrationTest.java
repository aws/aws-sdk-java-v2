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

package software.amazon.awssdk.services.s3.multipart;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.AnnotationDirective;
import software.amazon.awssdk.services.s3.model.AnnotationEntry;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectAnnotationResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectAnnotationsResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.TaggingDirective;


@Timeout(value = 3, unit = TimeUnit.MINUTES)
public class S3MultiPartCopyTagsAndAnnotationsIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(S3MultiPartCopyTagsAndAnnotationsIntegrationTest.class);
    private static final String SOURCE_KEY = "source-large-object";
    private static final String DEST_KEY = "dest-copied-object";
    // 20MB to force multipart copy (above the default 8MB threshold)
    private static final int OBJECT_SIZE = 20 * 1024 * 1024;

    private static S3AsyncClient multipartClient;

    @BeforeAll
    public static void init() throws Exception {
        setUp();
        createBucket(BUCKET);

        multipartClient = S3AsyncClient.builder()
                                       .multipartEnabled(true)
                                       .multipartConfiguration(c -> c.minimumPartSizeInBytes(8L * 1024 * 1024)
                                                                     .thresholdInBytes(8L * 1024 * 1024))
                                       .build();

        byte[] data = new byte[OBJECT_SIZE];
        ThreadLocalRandom.current().nextBytes(data);
        multipartClient.putObject(r -> r.bucket(BUCKET).key(SOURCE_KEY)
                                        .metadata(Collections.singletonMap("custom-meta", "source-value"))
                                        .contentType("application/octet-stream"),
                                  AsyncRequestBody.fromBytes(data)).join();

        s3.putObjectTagging(r -> r.bucket(BUCKET).key(SOURCE_KEY)
                                  .tagging(t -> t.tagSet(
                                      Tag.builder().key("env").value("test").build(),
                                      Tag.builder().key("team").value("sdk").build())));

        putAnnotation("annotation-1", "first-annotation-body");
        putAnnotation("annotation-2", "second-annotation-body");
        putAnnotation("annotation-3", "third-annotation-body");
    }

    @AfterAll
    public static void teardown() {
        deleteBucketAndAllContents(BUCKET);
        multipartClient.close();
    }

    private static CopyObjectRequest.Builder copyRequestBuilder(String destSuffix) {
        return CopyObjectRequest.builder()
                                .sourceBucket(BUCKET)
                                .sourceKey(SOURCE_KEY)
                                .destinationBucket(BUCKET)
                                .destinationKey(DEST_KEY + destSuffix);
    }

    private static void putAnnotation(String annotationName, String body) {
        multipartClient.putObjectAnnotation(
            r -> r.bucket(BUCKET).key(SOURCE_KEY).annotationName(annotationName),
            AsyncRequestBody.fromBytes(body.getBytes(StandardCharsets.UTF_8))).join();
    }

    @Test
    void multipartCopy_withTaggingDirectiveCopy_shouldCopyTags() {
        CopyObjectResponse response = multipartClient.copyObject(copyRequestBuilder("-tags")
                                                                     .metadataDirective(MetadataDirective.COPY)
                                                                     .taggingDirective(TaggingDirective.COPY)
                                                                     .build()).join();

        assertThat(response).isNotNull();

        GetObjectTaggingResponse tagging = s3.getObjectTagging(
            r -> r.bucket(BUCKET).key(DEST_KEY + "-tags"));
        assertThat(tagging.tagSet()).hasSize(2);
        assertThat(tagging.tagSet()).anySatisfy(tag -> {
            assertThat(tag.key()).isEqualTo("env");
            assertThat(tag.value()).isEqualTo("test");
        });
        assertThat(tagging.tagSet()).anySatisfy(tag -> {
            assertThat(tag.key()).isEqualTo("team");
            assertThat(tag.value()).isEqualTo("sdk");
        });
    }

    @Test
    void multipartCopy_withAnnotationDirectiveCopy_shouldCopyAllAnnotations() {
        CopyObjectResponse response = multipartClient.copyObject(copyRequestBuilder("-annotations")
                                                                     .metadataDirective(MetadataDirective.COPY)
                                                                     .annotationDirective(AnnotationDirective.COPY)
                                                                     .build()).join();

        assertThat(response).isNotNull();

        ListObjectAnnotationsResponse listResponse = multipartClient.listObjectAnnotations(
            r -> r.bucket(BUCKET).key(DEST_KEY + "-annotations")).join();
        List<AnnotationEntry> annotations = listResponse.annotations();
        assertThat(annotations).hasSize(3);

        ResponseBytes<GetObjectAnnotationResponse> anno1 = multipartClient.getObjectAnnotation(
            r -> r.bucket(BUCKET).key(DEST_KEY + "-annotations").annotationName("annotation-1"),
            AsyncResponseTransformer.toBytes()).join();
        assertThat(anno1.asUtf8String()).isEqualTo("first-annotation-body");

        ResponseBytes<GetObjectAnnotationResponse> anno2 = multipartClient.getObjectAnnotation(
            r -> r.bucket(BUCKET).key(DEST_KEY + "-annotations").annotationName("annotation-2"),
            AsyncResponseTransformer.toBytes()).join();
        assertThat(anno2.asUtf8String()).isEqualTo("second-annotation-body");

        ResponseBytes<GetObjectAnnotationResponse> anno3 = multipartClient.getObjectAnnotation(
            r -> r.bucket(BUCKET).key(DEST_KEY + "-annotations").annotationName("annotation-3"),
            AsyncResponseTransformer.toBytes()).join();
        assertThat(anno3.asUtf8String()).isEqualTo("third-annotation-body");
    }

    @Test
    void multipartCopy_withAllDirectives_shouldCopyMetadataTagsAndAnnotations() {
        CopyObjectResponse response = multipartClient.copyObject(copyRequestBuilder("-all")
                                                                     .metadataDirective(MetadataDirective.COPY)
                                                                     .taggingDirective(TaggingDirective.COPY)
                                                                     .annotationDirective(AnnotationDirective.COPY)
                                                                     .build()).join();

        assertThat(response).isNotNull();

        HeadObjectResponse headResponse = multipartClient.headObject(
            r -> r.bucket(BUCKET).key(DEST_KEY + "-all")).join();
        assertThat(headResponse.metadata()).containsEntry("custom-meta", "source-value");
        assertThat(headResponse.contentType()).isEqualTo("application/octet-stream");

        GetObjectTaggingResponse tagging = s3.getObjectTagging(
            r -> r.bucket(BUCKET).key(DEST_KEY + "-all"));
        assertThat(tagging.tagSet()).hasSize(2);

        ListObjectAnnotationsResponse listResponse = multipartClient.listObjectAnnotations(
            r -> r.bucket(BUCKET).key(DEST_KEY + "-all")).join();
        assertThat(listResponse.annotations()).hasSize(3);
    }

    @Test
    void multipartCopy_withoutDirectives_shouldNotCopyTagsOrAnnotations() {
        CopyObjectResponse response = multipartClient.copyObject(copyRequestBuilder("-nodirective")
                                                                     .build()).join();

        assertThat(response).isNotNull();

        GetObjectTaggingResponse tagging = s3.getObjectTagging(
            r -> r.bucket(BUCKET).key(DEST_KEY + "-nodirective"));
        assertThat(tagging.tagSet()).isEmpty();

        ListObjectAnnotationsResponse listResponse = multipartClient.listObjectAnnotations(
            r -> r.bucket(BUCKET).key(DEST_KEY + "-nodirective")).join();
        assertThat(listResponse.annotations()).isNullOrEmpty();
    }

    @Test
    void multipartCopy_withManyTags_shouldCopyAllTags() {
        String largeTagSourceKey = SOURCE_KEY + "-many-tags";
        byte[] data = new byte[OBJECT_SIZE];
        ThreadLocalRandom.current().nextBytes(data);
        multipartClient.putObject(r -> r.bucket(BUCKET).key(largeTagSourceKey),
                                  AsyncRequestBody.fromBytes(data)).join();

        List<Tag> largeTags = new ArrayList<>();
        String longValue = String.join("", Collections.nCopies(25, "abcdefghij"));
        for (int i = 0; i < 10; i++) {
            largeTags.add(Tag.builder().key("largekey" + i).value(longValue).build());
        }
        s3.putObjectTagging(r -> r.bucket(BUCKET).key(largeTagSourceKey)
                                  .tagging(t -> t.tagSet(largeTags)));

        CopyObjectResponse response = multipartClient.copyObject(CopyObjectRequest.builder()
                                                                                  .sourceBucket(BUCKET)
                                                                                  .sourceKey(largeTagSourceKey)
                                                                                  .destinationBucket(BUCKET)
                                                                                  .destinationKey(DEST_KEY + "-many-tags")
                                                                                  .metadataDirective(MetadataDirective.COPY)
                                                                                  .taggingDirective(TaggingDirective.COPY)
                                                                                  .build()).join();

        assertThat(response).isNotNull();

        GetObjectTaggingResponse tagging = s3.getObjectTagging(
            r -> r.bucket(BUCKET).key(DEST_KEY + "-many-tags"));
        assertThat(tagging.tagSet()).hasSize(10);
        for (int i = 0; i < 10; i++) {
            int idx = i;
            assertThat(tagging.tagSet()).anySatisfy(tag ->
                                                        assertThat(tag.key()).isEqualTo("largekey" + idx));
        }
    }
}