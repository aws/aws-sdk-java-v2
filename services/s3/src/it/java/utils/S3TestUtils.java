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

package utils;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.IoUtils;

public class S3TestUtils {

    public static void deleteBucketAndAllContents(S3Client s3, String bucketName) {
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


        ListObjectVersionsResponse versions = s3
                .listObjectVersions(ListObjectVersionsRequest.builder().bucket(bucketName).build());

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

    public static void assertMd5MatchesEtag(InputStream content, GetObjectResponse response) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = new DigestInputStream(content, md)) {
            IoUtils.drainInputStream(is);
        }
        byte[] expectedMd5 = BinaryUtils.fromHex(response.eTag().replace("\"", ""));
        byte[] calculatedMd5 = md.digest();
        if (!Arrays.equals(expectedMd5, calculatedMd5)) {
            throw new AssertionError(
                    String.format("Content malformed. Expected checksum was %s but calculated checksum was %s",
                                  BinaryUtils.toBase64(expectedMd5), BinaryUtils.toBase64(calculatedMd5)));
        }
    }
}
