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

package software.amazon.awssdk.services.s3.internal.extensions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

class DefaultS3ClientSdkExtensionTest {

    S3Client s3;

    @BeforeEach
    void setUp() {
        s3 = spy(S3Client.class);
    }

    @Test
    void doesBucketExist_200_returnsTrue() {
        stubHeadBucket(() -> HeadBucketResponse.builder().build());
        assertThat(s3.doesBucketExist("foo")).isEqualTo(true);
    }

    @Test
    void doesBucketExist_404_returnsFalse() {
        stubHeadBucket(() -> {
            throw NoSuchBucketException.builder().build();
        });
        assertThat(s3.doesBucketExist("foo")).isEqualTo(false);
    }

    @Test
    void doesBucketExist_403_propagatesException() {
        stubHeadBucket(() -> {
            throw S3Exception.builder().build();
        });
        assertThatThrownBy(() -> s3.doesBucketExist("foo")).isInstanceOf(S3Exception.class);
    }

    @Test
    void doesObjectExist_200_returnsTrue() {
        stubHeadObject(() -> HeadObjectResponse.builder().build());
        assertThat(s3.doesObjectExist("foo", "bar")).isEqualTo(true);
    }

    @Test
    void doesObjectExist_404_returnsFalse() {
        stubHeadObject(() -> {
            throw NoSuchKeyException.builder().build();
        });
        assertThat(s3.doesObjectExist("foo", "bar")).isEqualTo(false);
    }

    @Test
    void doesObjectExist_403_propagatesException() {
        stubHeadObject(() -> {
            throw S3Exception.builder().build();
        });
        assertThatThrownBy(() -> s3.doesObjectExist("foo", "bar")).isInstanceOf(S3Exception.class);
    }

    private void stubHeadBucket(Supplier<HeadBucketResponse> behavior) {
        doAnswer(i -> behavior.get()).when(s3).headBucket(any(HeadBucketRequest.class));
    }

    private void stubHeadObject(Supplier<HeadObjectResponse> behavior) {
        doAnswer(i -> behavior.get()).when(s3).headObject(any(HeadObjectRequest.class));
    }
}