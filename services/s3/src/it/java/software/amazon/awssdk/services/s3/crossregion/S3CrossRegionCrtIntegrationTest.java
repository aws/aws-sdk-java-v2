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

package software.amazon.awssdk.services.s3.crossregion;

import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;

public class S3CrossRegionCrtIntegrationTest extends S3CrossRegionAsyncIntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(S3CrossRegionCrtIntegrationTest.class);

    @BeforeAll
    static void setUpClass() {
        s3 = s3ClientBuilder().build();
        createBucket(BUCKET);
    }

    @AfterAll
    static void clearClass() {
        deleteBucketAndAllContents(BUCKET);
    }

    @BeforeEach
    public void initialize() {
        crossRegionS3Client = S3AsyncClient.crtBuilder()
                                           .region(CROSS_REGION)
                                           .crossRegionAccessEnabled(true)
                                             .build();
    }
    @Override
    protected String bucketName() {
        return BUCKET;
    }

}
