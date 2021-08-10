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

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.regions.Region;

public class S3ClientConfigurationTest {

    @Test
    public void nonPositiveMinimumPartSizeInBytes_shouldThrowException() {
        assertThatThrownBy(() -> S3ClientConfiguration.builder()
                                                      .minimumPartSizeInBytes(-10L)
                                                      .build())
            .hasMessageContaining("must be positive");
        assertThatThrownBy(() -> S3ClientConfiguration.builder()
                                                      .minimumPartSizeInBytes(0L)
                                                      .build())
            .hasMessageContaining("must be positive");

    }

    @Test
    public void nonPositiveTargetThroughput_shouldThrowException() {
        assertThatThrownBy(() -> S3ClientConfiguration.builder()
                                                      .targetThroughputInGbps(-10.0)
                                                      .build())
            .hasMessageContaining("must be positive");
        assertThatThrownBy(() -> S3ClientConfiguration.builder()
                                                      .targetThroughputInGbps(0.0)
                                                      .build())
            .hasMessageContaining("must be positive");
    }

    @Test
    public void nonPositiveMaxConcurrency_shouldThrowException() {
        assertThatThrownBy(() -> S3ClientConfiguration.builder()
                                                      .maxConcurrency(-10)
                                                      .build())
            .hasMessageContaining("must be positive");
        assertThatThrownBy(() -> S3ClientConfiguration.builder()
                                                      .maxConcurrency(0)
                                                      .build())
            .hasMessageContaining("must be positive");
    }

    @Test
    public void build_allProperties() {
        AwsCredentialsProvider credentials = () -> AwsBasicCredentials.create("test"
            , "test");
        S3ClientConfiguration configuration = S3ClientConfiguration.builder()
                                                                   .credentialsProvider(credentials)
                                                                   .maxConcurrency(100)
                                                                   .targetThroughputInGbps(10.0)
                                                                   .region(Region.US_WEST_2)
                                                                   .minimumPartSizeInBytes(5 * MB)
                                                                   .build();

        assertThat(configuration.credentialsProvider()).contains(credentials);
        assertThat(configuration.maxConcurrency()).contains(100);
        assertThat(configuration.region()).contains(Region.US_WEST_2);
        assertThat(configuration.targetThroughputInGbps()).contains(10.0);
        assertThat(configuration.minimumPartSizeInBytes()).contains(5 * MB);
    }

    @Test
    public void build_emptyBuilder() {
        S3ClientConfiguration configuration = S3ClientConfiguration.builder()
                                                                   .build();

        assertThat(configuration.credentialsProvider()).isEmpty();
        assertThat(configuration.maxConcurrency()).isEmpty();
        assertThat(configuration.region()).isEmpty();
        assertThat(configuration.targetThroughputInGbps()).isEmpty();
        assertThat(configuration.minimumPartSizeInBytes()).isEmpty();
    }

    @Test
    public void equalsHashCode() {
        AwsCredentialsProvider credentials = () -> AwsBasicCredentials.create("test"
            , "test");
        S3ClientConfiguration configuration1 = S3ClientConfiguration.builder()
                                                                    .credentialsProvider(credentials)
                                                                    .maxConcurrency(100)
                                                                    .targetThroughputInGbps(10.0)
                                                                    .region(Region.US_WEST_2)
                                                                    .minimumPartSizeInBytes(5 * MB)
                                                                    .build();

        S3ClientConfiguration configuration2 = S3ClientConfiguration.builder()
                                                                    .credentialsProvider(credentials)
                                                                    .maxConcurrency(100)
                                                                    .targetThroughputInGbps(10.0)
                                                                    .region(Region.US_WEST_2)
                                                                    .minimumPartSizeInBytes(5 * MB)
                                                                    .build();

        S3ClientConfiguration configuration3 = configuration1.toBuilder()
                                                             .credentialsProvider(AnonymousCredentialsProvider.create())
                                                             .maxConcurrency(50)
                                                             .targetThroughputInGbps(1.0)
                                                             .asyncConfiguration(c -> c.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR,
                                                                                                       Runnable::run))
                                                             .build();

        assertThat(configuration1).isEqualTo(configuration2);
        assertThat(configuration1.hashCode()).isEqualTo(configuration2.hashCode());
        assertThat(configuration1).isNotEqualTo(configuration3);
        assertThat(configuration1.hashCode()).isNotEqualTo(configuration3.hashCode());
    }
}
