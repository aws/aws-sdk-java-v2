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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.nio.charset.StandardCharsets;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class ExceptionUnmarshallingIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(ExceptionUnmarshallingIntegrationTest.class);

    private static final String KEY = "some-key";

    @BeforeClass
    public static void setupFixture() {
        createBucket(BUCKET);
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    public void createBucketAlreadyOwnedByYou() {
        assertThatThrownBy(() -> s3.createBucket(b -> b.bucket(BUCKET)))
            .isInstanceOf(BucketAlreadyOwnedByYouException.class)
            .satisfies(e -> assertMetadata((S3Exception) e, "BucketAlreadyOwnedByYou"));
    }

    @Test
    public void createBucketAlreadyExists() {
        assertThatThrownBy(() -> s3.createBucket(b -> b.bucket("development")))
            .isInstanceOf(BucketAlreadyExistsException.class)
            .satisfies(e -> assertMetadata((S3Exception) e, "BucketAlreadyExists"));
    }

    @Test
    public void getObjectNoSuchKey() {
        assertThatThrownBy(() -> s3.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).build()))
            .isInstanceOf(NoSuchKeyException.class)
            .satisfies(e -> assertMetadata((S3Exception) e, "NoSuchKey"));
    }

    @Test
    public void getObjectNoSuchBucket() {
        assertThatThrownBy(() -> s3.getObject(GetObjectRequest.builder().bucket(BUCKET + KEY).key(KEY).build()))
            .isInstanceOf(NoSuchBucketException.class)
            .satisfies(e -> assertMetadata((S3Exception) e, "NoSuchBucket"));
    }

    @Test
    public void getBucketPolicyNoSuchBucket() {
        assertThatThrownBy(() -> s3.getBucketPolicy(b -> b.bucket(BUCKET + KEY)))
            .isInstanceOf(NoSuchBucketException.class)
            .satisfies(e -> assertMetadata((S3Exception) e, "NoSuchBucket"));
    }

    @Test
    public void asyncGetBucketPolicyNoSuchBucket() {
        assertThatThrownBy(() -> s3Async.getBucketPolicy(b -> b.bucket(BUCKET + KEY)).join())
            .hasCauseExactlyInstanceOf(NoSuchBucketException.class)
            .satisfies(e -> assertMetadata((S3Exception) e.getCause(), "NoSuchBucket"));
    }

    @Test
    public void getObjectAclNoSuchKey() {
        assertThatThrownBy(() -> s3.getObjectAcl(b -> b.bucket(BUCKET).key(KEY)))
            .isInstanceOf(NoSuchKeyException.class)
            .satisfies(e -> assertMetadata((S3Exception) e, "NoSuchKey"));
    }

    @Test
    public void getObjectAclNoSuchBucket() {
        assertThatThrownBy(() -> s3.getObjectAcl(b -> b.bucket(BUCKET + KEY).key(KEY)))
            .isInstanceOf(NoSuchBucketException.class)
            .satisfies(e -> assertMetadata((S3Exception) e, "NoSuchBucket"));
    }

    @Test
    public void abortMultipartNoSuchUpload() {
        assertThatThrownBy(() -> s3.abortMultipartUpload(b -> b.bucket(BUCKET).key(KEY).uploadId("23232")))
            .isInstanceOf(NoSuchUploadException.class)
            .satisfies(e -> assertMetadata((S3Exception) e, "NoSuchUpload"));
    }

    @Test
    public void listObjectsWrongRegion() {
        assertThatThrownBy(() -> {
            try (S3Client client = s3ClientBuilder().region(Region.EU_CENTRAL_1).build()) {
                client.listObjectsV2(b -> b.bucket(BUCKET));
            }
        }).isExactlyInstanceOf(S3Exception.class) // Make sure it's not a modeled exception, because that's what we're testing
          .hasMessageContaining("The bucket you are attempting to access must be addressed using the specified endpoint.")
          .satisfies(e -> assertMetadata((S3Exception) e, "PermanentRedirect"));
    }

    @Test
    public void headObjectNoSuchKey() {
        assertThatThrownBy(() -> s3.headObject(b -> b.bucket(BUCKET).key(KEY)))
            .isInstanceOf(NoSuchKeyException.class)
            .satisfies(e -> assertMetadata((NoSuchKeyException) e, "NoSuchKey"))
            .satisfies(e -> assertThat(((NoSuchKeyException) e).statusCode()).isEqualTo(404));
    }

    @Test
    public void asyncHeadObjectNoSuchKey() {
        assertThatThrownBy(() -> s3Async.headObject(b -> b.bucket(BUCKET).key(KEY)).join())
            .hasCauseInstanceOf(NoSuchKeyException.class)
            .satisfies(e -> assertMetadata(((NoSuchKeyException) (e.getCause())), "NoSuchKey"))
            .satisfies(e -> assertThat(((NoSuchKeyException) (e.getCause())).statusCode()).isEqualTo(404));
    }

    @Test
    public void headBucketNoSuchBucket() {
        assertThatThrownBy(() -> s3.headBucket(b -> b.bucket(KEY)))
            .isInstanceOf(NoSuchBucketException.class)
            .satisfies(e -> assertMetadata((NoSuchBucketException) e, "NoSuchBucket"))
            .satisfies(e -> assertThat(((NoSuchBucketException) e).statusCode()).isEqualTo(404));
    }

    @Test
    public void asyncHeadBucketNoSuchBucket() {
        assertThatThrownBy(() -> s3Async.headBucket(b -> b.bucket(KEY)).join())
            .hasCauseInstanceOf(NoSuchBucketException.class)
            .satisfies(e -> assertMetadata(((NoSuchBucketException) (e.getCause())), "NoSuchBucket"))
            .satisfies(e -> assertThat(((NoSuchBucketException) (e.getCause())).statusCode()).isEqualTo(404));
    }

    @Test
    public void asyncHeadBucketWrongRegion() {
        assertThatThrownBy(() -> {
            try (S3AsyncClient s3AsyncClient = s3AsyncClientBuilder().region(Region.EU_CENTRAL_1).build()) {
                s3AsyncClient.headBucket(b -> b.bucket(BUCKET)).join();
            }
        }).hasCauseInstanceOf(S3Exception.class)
          .satisfies(e -> assertThat(((S3Exception) (e.getCause())).statusCode()).isEqualTo(301));
    }

    @Test
    @Ignore("TODO")
    public void errorResponseContainsRawBytes() {
        assertThatThrownBy(() -> s3.getObjectAcl(b -> b.bucket(BUCKET + KEY).key(KEY)))
            .isInstanceOf(NoSuchBucketException.class)
            .satisfies(e -> assertThat(
                ((NoSuchBucketException) e).awsErrorDetails().rawResponse().asString(StandardCharsets.UTF_8))
                .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Error><Code>NoSuchBucket</Code><Message>The "
                            + "specified bucket does not exist</Message>"));
    }

    private void assertMetadata(S3Exception e, String expectedErrorCode) {
        assertThat(e.awsErrorDetails()).satisfies(
            errorDetails -> {
                assertThat(errorDetails.errorCode()).isEqualTo(expectedErrorCode);
                assertThat(errorDetails.errorMessage()).isNotEmpty();
                assertThat(errorDetails.sdkHttpResponse()).isNotNull();
                assertThat(errorDetails.serviceName()).isEqualTo("S3");
            }
        );
        assertThat(e.requestId()).isNotEmpty();
    }
}
