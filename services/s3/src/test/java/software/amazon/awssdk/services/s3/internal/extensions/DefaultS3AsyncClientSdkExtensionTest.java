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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

class DefaultS3AsyncClientSdkExtensionTest {

    private S3AsyncClient s3;
    private DefaultS3AsyncClientSdkExtension extension;

    @BeforeEach
    void setUp() {
        s3 = mock(S3AsyncClient.class);
        extension = new DefaultS3AsyncClientSdkExtension(s3);
    }

    @Test
    void doesObjectExist_objectExists_returnsTrue() {
        when(s3.headObject(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder().build()));
        assertThat(extension.doesObjectExist("bucket", "key").join()).isTrue();
    }

    @Test
    void doesObjectExist_noSuchKey_returnsFalse() {
        CompletableFuture<HeadObjectResponse> future = new CompletableFuture<>();
        future.completeExceptionally(NoSuchKeyException.builder().build());
        when(s3.headObject(any(Consumer.class))).thenReturn(future);
        assertThat(extension.doesObjectExist("bucket", "key").join()).isFalse();
    }

    @Test
    void doesObjectExist_otherException_propagates() {
        S3Exception forbidden = (S3Exception) S3Exception.builder().statusCode(403).message("Forbidden").build();
        CompletableFuture<HeadObjectResponse> future = new CompletableFuture<>();
        future.completeExceptionally(forbidden);
        when(s3.headObject(any(Consumer.class))).thenReturn(future);
        assertThatThrownBy(() -> extension.doesObjectExist("bucket", "key").join())
            .isInstanceOf(CompletionException.class)
            .hasCause(forbidden);
    }

    @Test
    void doesBucketExist_bucketExists_returnsTrue() {
        when(s3.headBucket(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(HeadBucketResponse.builder().build()));
        assertThat(extension.doesBucketExist("bucket").join()).isTrue();
    }

    @Test
    void doesBucketExist_noSuchBucket_returnsFalse() {
        CompletableFuture<HeadBucketResponse> future = new CompletableFuture<>();
        future.completeExceptionally(NoSuchBucketException.builder().build());
        when(s3.headBucket(any(Consumer.class))).thenReturn(future);
        assertThat(extension.doesBucketExist("bucket").join()).isFalse();
    }

    @Test
    void doesBucketExist_otherException_propagates() {
        S3Exception forbidden = (S3Exception) S3Exception.builder().statusCode(403).message("Forbidden").build();
        CompletableFuture<HeadBucketResponse> future = new CompletableFuture<>();
        future.completeExceptionally(forbidden);
        when(s3.headBucket(any(Consumer.class))).thenReturn(future);
        assertThatThrownBy(() -> extension.doesBucketExist("bucket").join())
            .isInstanceOf(CompletionException.class)
            .hasCause(forbidden);
    }

    @Test
    void doesBucketExist_nullBucket_failsFuture() {
        CompletableFuture<Boolean> result = extension.doesBucketExist(null);
        assertThatThrownBy(result::join).hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void doesObjectExist_nullBucket_failsFuture() {
        CompletableFuture<Boolean> result = extension.doesObjectExist(null, "key");
        assertThatThrownBy(result::join).hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void doesObjectExist_nullKey_failsFuture() {
        CompletableFuture<Boolean> result = extension.doesObjectExist("bucket", null);
        assertThatThrownBy(result::join).hasCauseInstanceOf(NullPointerException.class);
    }
}
