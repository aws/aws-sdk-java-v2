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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3CrossRegionSyncIntegrationTest extends S3CrossRegionIntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(S3CrossRegionSyncIntegrationTest.class);
    private S3Client crossRegionS3Client;

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
        crossRegionS3Client = S3Client.builder()
                                      .region(Region.US_EAST_1)
                                      .crossRegionAccessEnabled(true)
                                      .build();
    }

    @Override
    protected List<S3Object> paginatedAPICall(ListObjectsV2Request listObjectsV2Request) {
        List<S3Object> resultS3Object = new ArrayList<>();
        Iterator<ListObjectsV2Response> v2ResponseIterator =
            crossRegionS3Client.listObjectsV2Paginator(listObjectsV2Request).iterator();
        while (v2ResponseIterator.hasNext()) {
            v2ResponseIterator.next().contents().forEach(a -> resultS3Object.add(a));
        }
        return resultS3Object;
    }

    @Override
    protected DeleteObjectsResponse postObjectAPICall(DeleteObjectsRequest deleteObjectsRequest) {
        return crossRegionS3Client.deleteObjects(deleteObjectsRequest);
    }

    @Override
    protected HeadBucketResponse headAPICall(HeadBucketRequest headBucketRequest) {
        return crossRegionS3Client.headBucket(headBucketRequest);
    }

    @Override
    protected DeleteObjectResponse deleteObjectAPICall(DeleteObjectRequest deleteObjectRequest) {
        return crossRegionS3Client.deleteObject(deleteObjectRequest);
    }

    @Override
    protected PutObjectResponse putAPICall(PutObjectRequest putObjectRequest, String testString) {
        return crossRegionS3Client.putObject(putObjectRequest, RequestBody.fromString(testString));
    }

    @Override
    protected ResponseBytes<GetObjectResponse> getAPICall(GetObjectRequest getObjectRequest) {
        return crossRegionS3Client.getObject(getObjectRequest, ResponseTransformer.toBytes());
    }

    @Override
    protected String bucketName() {
        return BUCKET;
    }
}
