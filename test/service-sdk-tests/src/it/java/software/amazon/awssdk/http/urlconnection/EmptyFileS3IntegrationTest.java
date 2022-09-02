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

package software.amazon.awssdk.http.urlconnection;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

public class EmptyFileS3IntegrationTest extends UrlHttpConnectionS3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(EmptyFileS3IntegrationTest.class);

    @BeforeAll
    public static void setup() {
        createBucket(BUCKET);
    }

    @AfterAll
    public static void cleanup() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    public void s3EmptyFileGetAsBytesWorksWithoutChecksumValidationEnabled() {
        try (S3Client s3 = s3ClientBuilder().serviceConfiguration(c -> c.checksumValidationEnabled(false))
                                            .build()) {
            s3.putObject(r -> r.bucket(BUCKET).key("x"), RequestBody.empty());
            assertThat(s3.getObjectAsBytes(r -> r.bucket(BUCKET).key("x")).asUtf8String()).isEmpty();
        }
    }

    @Test
    public void s3EmptyFileContentLengthIsCorrectWithoutChecksumValidationEnabled() {
        try (S3Client s3 = s3ClientBuilder().serviceConfiguration(c -> c.checksumValidationEnabled(false))
                                            .build()) {
            s3.putObject(r -> r.bucket(BUCKET).key("x"), RequestBody.empty());
            assertThat(s3.getObject(r -> r.bucket(BUCKET).key("x")).response().contentLength()).isEqualTo(0);
        }
    }
}
