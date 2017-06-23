/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.File;
import java.util.Iterator;
import java.util.List;
import org.junit.BeforeClass;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.test.AwsTestBase;
import software.amazon.awssdk.test.util.RandomTempFile;

/**
 * Base class for S3 integration tests. Loads AWS credentials from a properties
 * file and creates an S3 client for callers to use.
 */
public class S3IntegrationTestBase extends AwsTestBase {

    /** Android Directory, once set RandomTempFile will use it to create files. */
    public static File androidRootDir;
    /** The S3 client for all tests to use. */
    protected static S3Client s3;

    /**
     * Loads the AWS account info for the integration tests and creates an S3
     * client for tests to use.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        s3 = S3Client.builder().region(Region.US_WEST_2).credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    protected static File getRandomTempFile(String filename, long contentLength) throws Exception {
        if (androidRootDir == null) {
            return new RandomTempFile(filename, contentLength);
        } else {
            return new RandomTempFile(androidRootDir, filename, contentLength);
        }
    }

    public static void deleteBucketAndAllContents(String bucketName) {
        System.out.println("Deleting S3 bucket: " + bucketName);
        ListObjectsResponse response = s3.listObjects(ListObjectsRequest.builder().bucket(bucketName).build());
        List<S3Object> objectListing = response.contents();

        if (objectListing != null) {
            while (true) {
                for (Iterator<?> iterator = objectListing.iterator(); iterator.hasNext(); ) {
                    S3Object objectSummary = (S3Object) iterator.next();
                    s3.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(objectSummary.key()).build());
                }

                if (response.isTruncated()) {
                    objectListing = s3.listObjects(ListObjectsRequest.builder()
                                                                     .bucket(bucketName)
                                                                     .marker(response.marker())
                                                                     .build())
                                      .contents();
                } else {
                    break;
                }
            }
        }


        ListObjectVersionsResponse versions = s3.listObjectVersions(ListObjectVersionsRequest.builder().bucket(bucketName).build());

        if (versions.deleteMarkers() != null) {
            versions.deleteMarkers().forEach(v -> s3.deleteObject(DeleteObjectRequest.builder()
                                                                                     .versionId(v.versionId())
                                                                                     .bucket(bucketName)
                                                                                     .key(v.key())
                                                                                     .build()));
        }

        if (versions.versions() != null) {
            versions.versions().forEach(v -> s3.deleteObject(DeleteObjectRequest.builder()
                                                                                .versionId(v.versionId())
                                                                                .bucket(bucketName)
                                                                                .key(v.key())
                                                                                .build()));
        }

        s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
    }

}
