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

package software.amazon.awssdk.services.s3.extensions;

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
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

class S3ClientExtensionMethodsTest {

    TestS3Client s3Client;

    @BeforeEach
    void setUp() {
        s3Client = spy(TestS3Client.class);
    }

    @Test
    void doesBucketExist_200() {
        stubHeadBucket(() -> {
            return HeadBucketResponse.builder().build();
        });
        assertThat(s3Client.doesBucketExist("foo")).isEqualTo(true);
    }

    @Test
    void doesBucketExist_404() {
        stubHeadBucket(() -> {
            throw NoSuchBucketException.builder().build();
        });
        assertThat(s3Client.doesBucketExist("foo")).isEqualTo(false);
    }

    @Test
    void doesBucketExist_403() {
        stubHeadBucket(() -> {
            throw S3Exception.builder().build();
        });
        assertThatThrownBy(() -> s3Client.doesBucketExist("foo")).isInstanceOf(S3Exception.class);
    }

    private void stubHeadBucket(Supplier<HeadBucketResponse> behavior) {
        doAnswer(i -> behavior.get()).when(s3Client).headBucket(any(HeadBucketRequest.class));
    }

    static class TestS3Client implements S3Client, S3ClientExtensionMethods {

        @Override
        public String serviceName() {
            return null;
        }

        @Override
        public void close() {

        }

        TestS3Client() {
        }
    }
}