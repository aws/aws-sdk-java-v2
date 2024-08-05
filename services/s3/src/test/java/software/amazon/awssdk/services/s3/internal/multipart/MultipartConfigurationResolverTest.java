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

package software.amazon.awssdk.services.s3.internal.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;

public class MultipartConfigurationResolverTest {

    @Test
    void resolveThresholdInBytes_valueNotProvided_shouldSameAsPartSize() {
        MultipartConfiguration configuration = MultipartConfiguration.builder()
                                                                     .minimumPartSizeInBytes(10L)
                                                                     .build();
        MultipartConfigurationResolver resolver = new MultipartConfigurationResolver(configuration);
        assertThat(resolver.thresholdInBytes()).isEqualTo(10L);
    }

    @Test
    void resolveThresholdInBytes_valueProvided_shouldHonor() {
        MultipartConfiguration configuration = MultipartConfiguration.builder()
                                                                     .minimumPartSizeInBytes(1L)
                                                                     .thresholdInBytes(12L)
                                                                     .build();
        MultipartConfigurationResolver resolver = new MultipartConfigurationResolver(configuration);
        assertThat(resolver.thresholdInBytes()).isEqualTo(12L);
    }

    @Test
    void resolveApiCallBufferSize_valueProvided_shouldHonor() {
        MultipartConfiguration configuration = MultipartConfiguration.builder()
                                                                     .apiCallBufferSizeInBytes(100L)
                                                                     .build();
        MultipartConfigurationResolver resolver = new MultipartConfigurationResolver(configuration);
        assertThat(resolver.apiCallBufferSize()).isEqualTo(100L);
    }

    @Test
    void resolveApiCallBufferSize_valueNotProvided_shouldComputeBasedOnPartSize() {
        MultipartConfiguration configuration = MultipartConfiguration.builder()
                                                                     .minimumPartSizeInBytes(10L)
                                                                     .build();
        MultipartConfigurationResolver resolver = new MultipartConfigurationResolver(configuration);
        assertThat(resolver.apiCallBufferSize()).isEqualTo(40L);
    }

    @Test
    void valueProvidedForAllFields_shouldHonor() {
        MultipartConfiguration configuration = MultipartConfiguration.builder()
                                                                     .minimumPartSizeInBytes(10L)
                                                                     .thresholdInBytes(8L)
                                                                     .apiCallBufferSizeInBytes(3L)
                                                                     .build();
        MultipartConfigurationResolver resolver = new MultipartConfigurationResolver(configuration);
        assertThat(resolver.minimalPartSizeInBytes()).isEqualTo(10L);
        assertThat(resolver.thresholdInBytes()).isEqualTo(8L);
        assertThat(resolver.apiCallBufferSize()).isEqualTo(3L);
    }

    @Test
    void noValueProvided_shouldUseDefault() {
        MultipartConfigurationResolver resolver = new MultipartConfigurationResolver(MultipartConfiguration.builder()
                                                                                                           .build());
        assertThat(resolver.minimalPartSizeInBytes()).isEqualTo(8L * 1024 * 1024);
        assertThat(resolver.thresholdInBytes()).isEqualTo(8L * 1024 * 1024);
        assertThat(resolver.apiCallBufferSize()).isEqualTo(8L * 1024 * 1024 * 4);
    }
}
