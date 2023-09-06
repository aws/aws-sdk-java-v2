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

package software.amazon.awssdk.services.oldclient;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class S3ClientTest {
    S3Client s3 = S3Client.create();
    S3AsyncClient s3Async = S3AsyncClient.create();
    String bucket = "millem-test-bucket-102390129";

    @Test
    public void create() {
        s3.createBucket(r -> r.bucket(bucket));
    }

    @Test
    public void putGet() {
        s3.putObject(r -> r.bucket(bucket).key("key1/key2"), RequestBody.fromString("foo"));
        assertThat(s3.getObjectAsBytes(r -> r.bucket(bucket).key("key1/key2")).asUtf8String()).isEqualTo("foo");
    }

    @Test
    public void putGet_async() {
        s3Async.putObject(r -> r.bucket(bucket).key("key1/key2"), AsyncRequestBody.fromString("foo")).join();
        assertThat(s3Async.getObject(r -> r.bucket(bucket).key("key1/key2"),
                                     AsyncResponseTransformer.toBytes())
                          .join()
                          .asUtf8String()).isEqualTo("foo");
    }

    @Test
    public void putGetFlexibleChecksums() {
        s3.putObject(r -> r.bucket(bucket).key("key1/key2").checksumAlgorithm(ChecksumAlgorithm.CRC32),
                          RequestBody.fromString("foo"));
        ResponseBytes<GetObjectResponse> response =
            s3.getObjectAsBytes(r -> r.bucket(bucket).key("key1/key2").checksumMode(ChecksumMode.ENABLED));
        assertThat(response.asUtf8String()).isEqualTo("foo");
        assertThat(response.response().checksumCRC32()).isEqualTo("jHNlIQ==");
    }

    @Test
    public void putGetFlexibleChecksums_async() {
        s3Async.putObject(r -> r.bucket(bucket).key("key1/key2").checksumAlgorithm(ChecksumAlgorithm.CRC32),
                          AsyncRequestBody.fromString("foo")).join();
        ResponseBytes<GetObjectResponse> response = s3Async.getObject(r -> r.bucket(bucket).key("key1/key2").checksumMode(ChecksumMode.ENABLED),
                                                                      AsyncResponseTransformer.toBytes())
                                                           .join();
        assertThat(response.asUtf8String()).isEqualTo("foo");
        assertThat(response.response().checksumCRC32()).isEqualTo("jHNlIQ==");
    }
}
