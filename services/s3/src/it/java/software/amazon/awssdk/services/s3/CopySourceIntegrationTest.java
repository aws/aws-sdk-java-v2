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

package software.amazon.awssdk.services.s3;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.internal.handlers.CopySourceInterceptor;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Integration tests for the {@code sourceBucket}, {@code sourceKey}, and {@code sourceVersionId} parameters for
 * {@link CopyObjectRequest}. Specifically, we ensure that users are able to seamlessly use the same input for both the
 * {@link PutObjectRequest} key and the {@link CopyObjectRequest} source key (and not be required to manually URL encode the
 * COPY source key). This also effectively tests for parity with the SDK v1 behavior.
 *
 * @see CopySourceInterceptor
 */
@RunWith(Parameterized.class)
public class CopySourceIntegrationTest extends S3IntegrationTestBase {

    private static final String SOURCE_UNVERSIONED_BUCKET_NAME = temporaryBucketName("copy-source-integ-test-src");
    private static final String SOURCE_VERSIONED_BUCKET_NAME = temporaryBucketName("copy-source-integ-test-versioned-src");
    private static final String DESTINATION_BUCKET_NAME = temporaryBucketName("copy-source-integ-test-dest");

    @BeforeClass
    public static void initializeTestData() throws Exception {
        createBucket(SOURCE_UNVERSIONED_BUCKET_NAME);
        createBucket(SOURCE_VERSIONED_BUCKET_NAME);
        s3.putBucketVersioning(r -> r
            .bucket(SOURCE_VERSIONED_BUCKET_NAME)
            .versioningConfiguration(v -> v.status(BucketVersioningStatus.ENABLED)));
        createBucket(DESTINATION_BUCKET_NAME);
    }

    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(SOURCE_UNVERSIONED_BUCKET_NAME);
        deleteBucketAndAllContents(SOURCE_VERSIONED_BUCKET_NAME);
        deleteBucketAndAllContents(DESTINATION_BUCKET_NAME);
    }

    @Parameters
    public static Collection<String> parameters() throws Exception {
        return Arrays.asList(
            "simpleKey",
            "key/with/slashes",
            "\uD83E\uDEA3",
            "specialChars/ +!#$&'()*,:;=?@\"",
            "%20"
        );
    }

    private final String key;

    public CopySourceIntegrationTest(String key) {
        this.key = key;
    }

    @Test
    public void copyObject_WithoutVersion_AcceptsSameKeyAsPut() throws Exception {
        String originalContent = UUID.randomUUID().toString();

        s3.putObject(PutObjectRequest.builder()
                                     .bucket(SOURCE_UNVERSIONED_BUCKET_NAME)
                                     .key(key)
                                     .build(), RequestBody.fromString(originalContent, StandardCharsets.UTF_8));

        s3.copyObject(CopyObjectRequest.builder()
                                       .sourceBucket(SOURCE_UNVERSIONED_BUCKET_NAME)
                                       .sourceKey(key)
                                       .destinationBucket(DESTINATION_BUCKET_NAME)
                                       .destinationKey(key)
                                       .build());

        String copiedContent = s3.getObjectAsBytes(GetObjectRequest.builder()
                                                                   .bucket(DESTINATION_BUCKET_NAME)
                                                                   .key(key)
                                                                   .build()).asUtf8String();

        assertThat(copiedContent, is(originalContent));
    }

    /**
     * Test that we can correctly copy versioned source objects.
     * <p>
     * Motivated by: https://github.com/aws/aws-sdk-js/issues/727
     */
    @Test
    public void copyObject_WithVersion_AcceptsSameKeyAsPut() throws Exception {
        Map<String, String> versionToContentMap = new HashMap<>();
        int numVersionsToCreate = 3;
        for (int i = 0; i < numVersionsToCreate; i++) {
            String originalContent = UUID.randomUUID().toString();
            PutObjectResponse response = s3.putObject(PutObjectRequest.builder()
                                                                      .bucket(SOURCE_VERSIONED_BUCKET_NAME)
                                                                      .key(key)
                                                                      .build(),
                                                      RequestBody.fromString(originalContent, StandardCharsets.UTF_8));
            versionToContentMap.put(response.versionId(), originalContent);
        }

        versionToContentMap.forEach((versionId, originalContent) -> {
            s3.copyObject(CopyObjectRequest.builder()
                                           .sourceBucket(SOURCE_VERSIONED_BUCKET_NAME)
                                           .sourceKey(key)
                                           .sourceVersionId(versionId)
                                           .destinationBucket(DESTINATION_BUCKET_NAME)
                                           .destinationKey(key)
                                           .build());

            String copiedContent = s3.getObjectAsBytes(GetObjectRequest.builder()
                                                                       .bucket(DESTINATION_BUCKET_NAME)
                                                                       .key(key)
                                                                       .build()).asUtf8String();
            assertThat(copiedContent, is(originalContent));
        });
    }
}
