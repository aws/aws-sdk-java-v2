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

package software.amazon.awssdk.services.s3.s3express;

import static software.amazon.awssdk.testutils.service.AwsTestBase.CREDENTIALS_PROVIDER_CHAIN;

import java.util.Iterator;
import java.util.List;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.BucketInfo;
import software.amazon.awssdk.services.s3.model.BucketType;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DataRedundancy;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.LocationInfo;
import software.amazon.awssdk.services.s3.model.LocationType;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.utils.Logger;

public class S3ExpressIntegrationTestBase {
    private static final Logger log = Logger.loggerFor(S3ExpressIntegrationTestBase.class);

    protected static S3ClientBuilder s3ClientBuilder(Region region) {
        return S3Client.builder()
                       .region(region)
                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN);

    }

    protected static S3AsyncClientBuilder s3AsyncClientBuilder(Region region) {
        return S3AsyncClient.builder()
                            .region(region)
                            .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN);

    }

    protected static S3CrtAsyncClientBuilder s3CrtAsyncClientBuilder(Region region) {
        return S3AsyncClient.crtBuilder()
                            .region(region)
                            .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN);

    }

    protected static void createBucketS3Express(S3Client client, String bucketName, String az) {
        try {
            LocationInfo location = LocationInfo.builder().name(az).type(LocationType.AVAILABILITY_ZONE).build();
            BucketInfo bucketInfo = BucketInfo.builder()
                                              .dataRedundancy(DataRedundancy.SINGLE_AVAILABILITY_ZONE)
                                              .type(BucketType.DIRECTORY)
                                              .build();
            CreateBucketConfiguration bucketConfiguration = CreateBucketConfiguration.builder()
                                                                                     .location(location)
                                                                                     .bucket(bucketInfo)
                                                                                     .build();
            client.createBucket(CreateBucketRequest.builder()
                                                   .bucket(bucketName)
                                                   .createBucketConfiguration(bucketConfiguration)
                                                   .build());
        } catch (S3Exception e) {
           log.error(() -> "Error attempting to create bucket: " + bucketName, e);
            if (e.awsErrorDetails().errorCode().equals("BucketAlreadyOwnedByYou")) {
                log.error(() -> bucketName + " bucket already exists, likely leaked by a previous run");
            } else {
                throw e;
            }
        }
        client.waiter().waitUntilBucketExists(r -> r.bucket(bucketName));
    }

    protected static void deleteBucketAndAllContents(S3Client s3, String bucketName) {
        try {
            System.out.println("Deleting S3 bucket: " + bucketName);
            ListObjectsV2Response response = Waiter.run(() -> s3.listObjectsV2(r -> r.bucket(bucketName)))
                                                   .ignoringException(NoSuchBucketException.class)
                                                   .orFail();
            List<S3Object> objectListing = response.contents();

            if (objectListing != null) {
                while (true) {
                    for (Iterator<?> iterator = objectListing.iterator(); iterator.hasNext(); ) {
                        S3Object objectSummary = (S3Object) iterator.next();
                        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(objectSummary.key()).build());
                    }

                    if (response.isTruncated()) {
                        objectListing = s3.listObjectsV2(ListObjectsV2Request.builder()
                                                                             .bucket(bucketName)
                                                                             .continuationToken(response.continuationToken())
                                                                             .build())
                                          .contents();
                    } else {
                        break;
                    }
                }
            }

            s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
        } catch (Exception e) {
            System.err.println("Failed to delete bucket: " + bucketName);
            e.printStackTrace();
        }
    }
}
