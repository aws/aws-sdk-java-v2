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

package software.amazon.awssdk.services.s3.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartS3AsyncClient;

class S3MultipartClientBuilderTest {

    @Test
    void builderWithMultipartConfig_shouldBuildMultipartClient() {
        S3AsyncClient client = S3AsyncClient.builder()
                                            .multipartConfiguration(MultipartConfiguration.builder().build())
                                            .region(Region.US_EAST_1)
                                            .build();
        assertThat(client).isInstanceOf(MultipartS3AsyncClient.class);

    }

    @Test
    void builderWithMultipartConfig_consumerBuilder_shouldBuildMultipartClient() {
        S3AsyncClient client = S3AsyncClient.builder()
                                            .multipartConfiguration(b -> b.multipartEnabled(true))
                                            .region(Region.US_EAST_1)
                                            .build();
        assertThat(client).isInstanceOf(MultipartS3AsyncClient.class);
    }

    @Test
    void builderWithMultipartConfig_disabled_shouldNotBuildMultipartClient() {
        S3AsyncClient client = S3AsyncClient.builder()
                                            .multipartConfiguration(b -> b.multipartEnabled(false))
                                            .region(Region.US_EAST_1)
                                            .build();
        assertThat(client).isNotInstanceOf(MultipartS3AsyncClient.class);
    }

    @Test
    void builderWithoutMultipartConfig_shouldNotBeMultipartClient() {
        S3AsyncClient client = S3AsyncClient.builder()
                                            .region(Region.US_EAST_1)
                                            .build();
        assertThat(client).isNotInstanceOf(MultipartS3AsyncClient.class);
    }
}
