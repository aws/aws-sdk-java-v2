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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

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

    S3AsyncClient s3;

    @BeforeEach
    void setUp() {
        s3 = spy(S3AsyncClient.class);
    }

    @Test
    void doesObjectExist_200_returnsTrue() {
        stubHeadObject(CompletableFuture.completedFuture(HeadObjectResponse.builder().build()));
        assertThat(s3.doesObjectExist("foo", "bar").join()).isTrue();
    }

    @Test
    void doesObjectExist_404_returnsFalse() {
        CompletableFuture<HeadObjectResponse> future = new CompletableFuture<>();
        future.completeExceptionally(NoSuchKeyException.builder().build());
        stubHeadObject(future);
        assertThat(s3.doesObjectExist("foo", "bar").join()).isFalse();
    }

    @Test
    void doesObjectExist_403_propagatesException() {
        S3Exception forbidden = (S3Exception) S3Exception.builder().statusCode(403).message("Forbidden").build();
        CompletableFuture<HeadObjectResponse> future = new CompletableFuture<>();
        future.completeExceptionally(forbidden);
        stubHeadObject(future);
        assertThatThrownBy(() -> s3.doesObjectExist("foo", "bar").join())
            .isInstanceOf(CompletionException.class)
            .hasCause(forbidden);
    }

    @Test
    void doesBucketExist_200_returnsTrue() {
        stubHeadBucket(CompletableFuture.completedFuture(HeadBucketResponse.builder().build()));
        assertThat(s3.doesBucketExist("foo").join()).isTrue();
    }

    @Test
    void doesBucketExist_404_returnsFalse() {
        CompletableFuture<HeadBucketResponse> future = new CompletableFuture<>();
        future.completeExceptionally(NoSuchBucketException.builder().build());
        stubHeadBucket(future);
        assertThat(s3.doesBucketExist("foo").join()).isFalse();
    }

    @Test
    void doesBucketExist_403_propagatesException() {
        S3Exception forbidden = (S3Exception) S3Exception.builder().statusCode(403).message("Forbidden").build();
        CompletableFuture<HeadBucketResponse> future = new CompletableFuture<>();
        future.completeExceptionally(forbidden);
        stubHeadBucket(future);
        assertThatThrownBy(() -> s3.doesBucketExist("foo").join())
            .isInstanceOf(CompletionException.class)
            .hasCause(forbidden);
    }

    // Validation tests

    @Test
    void doesBucketExist_nullBucket_fails() {
        assertThatThrownBy(() -> s3.doesBucketExist(null).join())
            .hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void doesBucketExist_emptyBucket_fails() {
        assertThatThrownBy(() -> s3.doesBucketExist("").join())
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesObjectExist_nullBucket_fails() {
        assertThatThrownBy(() -> s3.doesObjectExist(null, "key").join())
            .hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void doesObjectExist_emptyBucket_fails() {
        assertThatThrownBy(() -> s3.doesObjectExist("", "key").join())
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesObjectExist_nullKey_fails() {
        assertThatThrownBy(() -> s3.doesObjectExist("bucket", null).join())
            .hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void doesObjectExist_emptyKey_fails() {
        assertThatThrownBy(() -> s3.doesObjectExist("bucket", "").join())
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private void stubHeadBucket(CompletableFuture<HeadBucketResponse> result) {
        doReturn(result).when(s3).headBucket(any(Consumer.class));
    }

    private void stubHeadObject(CompletableFuture<HeadObjectResponse> result) {
        doReturn(result).when(s3).headObject(any(Consumer.class));
    }
}
