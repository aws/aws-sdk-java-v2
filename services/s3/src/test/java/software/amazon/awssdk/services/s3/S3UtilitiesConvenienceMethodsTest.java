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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class S3UtilitiesConvenienceMethodsTest {

    private final S3Client syncClient = mock(S3Client.class);
    private final S3AsyncClient asyncClient = mock(S3AsyncClient.class);
    private final S3Utilities syncUtilities = S3Utilities.create(mockClientConfiguration(), syncClient);
    private final S3Utilities asyncUtilities = S3Utilities.create(mockClientConfiguration(), asyncClient);

    // --- doesObjectExist (sync) ---

    @Test
    public void doesObjectExist_headObjectSucceeds_returnsTrue() {
        when(syncClient.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());
        assertThat(syncUtilities.doesObjectExist("bucket", "key")).isTrue();
    }

    @Test
    public void doesObjectExist_noSuchKey_returnsFalse() {
        when(syncClient.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());
        assertThat(syncUtilities.doesObjectExist("bucket", "key")).isFalse();
    }

    @Test
    public void doesObjectExist_otherException_propagates() {
        when(syncClient.headObject(any(HeadObjectRequest.class)))
            .thenThrow((S3Exception) S3Exception.builder().statusCode(403).message("Forbidden").build());
        assertThatThrownBy(() -> syncUtilities.doesObjectExist("bucket", "key"))
            .isInstanceOf(S3Exception.class);
    }

    // --- doesBucketExist (sync) ---

    @Test
    public void doesBucketExist_headBucketSucceeds_returnsTrue() {
        when(syncClient.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());
        assertThat(syncUtilities.doesBucketExist("bucket")).isTrue();
    }

    @Test
    public void doesBucketExist_noSuchBucket_returnsFalse() {
        when(syncClient.headBucket(any(HeadBucketRequest.class))).thenThrow(NoSuchBucketException.builder().build());
        assertThat(syncUtilities.doesBucketExist("bucket")).isFalse();
    }

    @Test
    public void doesBucketExist_otherException_propagates() {
        when(syncClient.headBucket(any(HeadBucketRequest.class)))
            .thenThrow((S3Exception) S3Exception.builder().statusCode(403).message("Forbidden").build());
        assertThatThrownBy(() -> syncUtilities.doesBucketExist("bucket"))
            .isInstanceOf(S3Exception.class);
    }

    // --- doesObjectExistAsync ---

    @Test
    public void doesObjectExistAsync_headObjectSucceeds_returnsTrue() {
        when(asyncClient.headObject(any(HeadObjectRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder().build()));
        assertThat(asyncUtilities.doesObjectExistAsync("bucket", "key").join()).isTrue();
    }

    @Test
    public void doesObjectExistAsync_noSuchKey_returnsFalse() {
        CompletableFuture<HeadObjectResponse> future = new CompletableFuture<>();
        future.completeExceptionally(NoSuchKeyException.builder().build());
        when(asyncClient.headObject(any(HeadObjectRequest.class))).thenReturn(future);
        assertThat(asyncUtilities.doesObjectExistAsync("bucket", "key").join()).isFalse();
    }

    @Test
    public void doesObjectExistAsync_otherException_propagates() {
        CompletableFuture<HeadObjectResponse> future = new CompletableFuture<>();
        future.completeExceptionally((S3Exception) S3Exception.builder().statusCode(403).message("Forbidden").build());
        when(asyncClient.headObject(any(HeadObjectRequest.class))).thenReturn(future);
        assertThatThrownBy(() -> asyncUtilities.doesObjectExistAsync("bucket", "key").join())
            .hasCauseInstanceOf(S3Exception.class);
    }

    // --- doesBucketExistAsync ---

    @Test
    public void doesBucketExistAsync_headBucketSucceeds_returnsTrue() {
        when(asyncClient.headBucket(any(HeadBucketRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(HeadBucketResponse.builder().build()));
        assertThat(asyncUtilities.doesBucketExistAsync("bucket").join()).isTrue();
    }

    @Test
    public void doesBucketExistAsync_noSuchBucket_returnsFalse() {
        CompletableFuture<HeadBucketResponse> future = new CompletableFuture<>();
        future.completeExceptionally(NoSuchBucketException.builder().build());
        when(asyncClient.headBucket(any(HeadBucketRequest.class))).thenReturn(future);
        assertThat(asyncUtilities.doesBucketExistAsync("bucket").join()).isFalse();
    }

    @Test
    public void doesBucketExistAsync_otherException_propagates() {
        CompletableFuture<HeadBucketResponse> future = new CompletableFuture<>();
        future.completeExceptionally((S3Exception) S3Exception.builder().statusCode(403).message("Forbidden").build());
        when(asyncClient.headBucket(any(HeadBucketRequest.class))).thenReturn(future);
        assertThatThrownBy(() -> asyncUtilities.doesBucketExistAsync("bucket").join())
            .hasCauseInstanceOf(S3Exception.class);
    }

    // --- validation ---

    @Test
    public void doesObjectExist_nullBucket_throws() {
        assertThatThrownBy(() -> syncUtilities.doesObjectExist(null, "key"))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void doesObjectExist_emptyKey_throws() {
        assertThatThrownBy(() -> syncUtilities.doesObjectExist("bucket", ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void doesBucketExist_emptyBucket_throws() {
        assertThatThrownBy(() -> syncUtilities.doesBucketExist(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void doesObjectExist_standaloneUtilities_throwsIllegalState() {
        S3Utilities standalone = S3Utilities.builder().region(Region.US_EAST_1).build();
        assertThatThrownBy(() -> standalone.doesObjectExist("bucket", "key"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void doesObjectExistAsync_fromSyncClient_throwsIllegalState() {
        assertThatThrownBy(() -> syncUtilities.doesObjectExistAsync("bucket", "key"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void doesObjectExist_fromAsyncClient_throwsIllegalState() {
        assertThatThrownBy(() -> asyncUtilities.doesObjectExist("bucket", "key"))
            .isInstanceOf(IllegalStateException.class);
    }

    private static SdkClientConfiguration mockClientConfiguration() {
        return SdkClientConfiguration.builder()
                   .option(AwsClientOption.AWS_REGION, Region.US_EAST_1)
                   .option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER,
                           ClientEndpointProvider.forEndpointOverride(URI.create("https://s3.us-east-1.amazonaws.com")))
                   .build();
    }
}
