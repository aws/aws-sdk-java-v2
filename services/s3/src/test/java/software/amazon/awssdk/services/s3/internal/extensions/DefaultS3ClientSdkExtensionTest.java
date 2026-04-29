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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
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

    private S3Client s3;
    private DefaultS3ClientSdkExtension extension;

    @BeforeEach
    void setUp() {
        s3 = mock(S3Client.class);
        extension = new DefaultS3ClientSdkExtension(s3);
    }

    @Test
    void doesObjectExist_objectExists_returnsTrue() {
        when(s3.headObject(any(Consumer.class))).thenReturn(HeadObjectResponse.builder().build());
        assertThat(extension.doesObjectExist("bucket", "key")).isTrue();
    }

    @Test
    void doesObjectExist_noSuchKey_returnsFalse() {
        when(s3.headObject(any(Consumer.class))).thenThrow(NoSuchKeyException.builder().build());
        assertThat(extension.doesObjectExist("bucket", "key")).isFalse();
    }

    @Test
    void doesObjectExist_otherS3Exception_propagates() {
        S3Exception forbidden = (S3Exception) S3Exception.builder().statusCode(403).message("Forbidden").build();
        when(s3.headObject(any(Consumer.class))).thenThrow(forbidden);
        assertThatThrownBy(() -> extension.doesObjectExist("bucket", "key")).isSameAs(forbidden);
    }

    @Test
    void doesBucketExist_bucketExists_returnsTrue() {
        when(s3.headBucket(any(Consumer.class))).thenReturn(HeadBucketResponse.builder().build());
        assertThat(extension.doesBucketExist("bucket")).isTrue();
    }

    @Test
    void doesBucketExist_noSuchBucket_returnsFalse() {
        when(s3.headBucket(any(Consumer.class))).thenThrow(NoSuchBucketException.builder().build());
        assertThat(extension.doesBucketExist("bucket")).isFalse();
    }

    @Test
    void doesBucketExist_otherS3Exception_propagates() {
        S3Exception forbidden = (S3Exception) S3Exception.builder().statusCode(403).message("Forbidden").build();
        when(s3.headBucket(any(Consumer.class))).thenThrow(forbidden);
        assertThatThrownBy(() -> extension.doesBucketExist("bucket")).isSameAs(forbidden);
    }

    @Test
    void doesBucketExist_nullBucket_throws() {
        assertThatThrownBy(() -> extension.doesBucketExist(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void doesBucketExist_emptyBucket_throws() {
        assertThatThrownBy(() -> extension.doesBucketExist("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesObjectExist_nullBucket_throws() {
        assertThatThrownBy(() -> extension.doesObjectExist(null, "key")).isInstanceOf(NullPointerException.class);
    }

    @Test
    void doesObjectExist_emptyBucket_throws() {
        assertThatThrownBy(() -> extension.doesObjectExist("", "key")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesObjectExist_nullKey_throws() {
        assertThatThrownBy(() -> extension.doesObjectExist("bucket", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void doesObjectExist_emptyKey_throws() {
        assertThatThrownBy(() -> extension.doesObjectExist("bucket", "")).isInstanceOf(IllegalArgumentException.class);
    }
}
