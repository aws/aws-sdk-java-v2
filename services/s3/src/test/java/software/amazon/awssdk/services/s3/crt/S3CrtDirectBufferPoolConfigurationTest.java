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

package software.amazon.awssdk.services.s3.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class S3CrtDirectBufferPoolConfigurationTest {

    @Test
    void equalsHashCode() {
        EqualsVerifier.forClass(S3CrtDirectBufferPoolConfiguration.class)
                      .verify();
    }

    @Test
    void auto_shouldCreateAutomaticConfiguration() {
        S3CrtDirectBufferPoolConfiguration configuration = S3CrtDirectBufferPoolConfiguration.auto();

        assertThat(configuration.minimumSizeInBytes()).isNull();
        assertThat(configuration.memoryLimitInBytes()).isNull();
    }

    @Test
    void fixed_shouldCreateFixedConfiguration() {
        S3CrtDirectBufferPoolConfiguration configuration = S3CrtDirectBufferPoolConfiguration.fixed(1024);

        assertThat(configuration.minimumSizeInBytes()).isNull();
        assertThat(configuration.memoryLimitInBytes()).isEqualTo(1024);
        assertThat(configuration.toString()).contains("1024");
    }

    @Test
    void elastic_shouldCreateElasticConfiguration() {
        S3CrtDirectBufferPoolConfiguration configuration = S3CrtDirectBufferPoolConfiguration.elastic(0, 1024);

        assertThat(configuration.minimumSizeInBytes()).isZero();
        assertThat(configuration.memoryLimitInBytes()).isEqualTo(1024);
        assertThat(configuration).isNotEqualTo(S3CrtDirectBufferPoolConfiguration.fixed(1024));
        assertThat(configuration.toString()).contains("minimumSizeInBytes=0", "memoryLimitInBytes=1024");
    }

    @Test
    void elastic_whenMinimumEqualsMemoryLimit_shouldRemainDistinctFromFixed() {
        S3CrtDirectBufferPoolConfiguration configuration = S3CrtDirectBufferPoolConfiguration.elastic(1024, 1024);
        S3CrtDirectBufferPoolConfiguration equalConfiguration = S3CrtDirectBufferPoolConfiguration.elastic(1024, 1024);

        assertThat(configuration.minimumSizeInBytes()).isEqualTo(1024);
        assertThat(configuration.memoryLimitInBytes()).isEqualTo(1024);
        assertThat(configuration).isEqualTo(equalConfiguration)
                              .hasSameHashCodeAs(equalConfiguration)
                              .isNotEqualTo(S3CrtDirectBufferPoolConfiguration.fixed(1024));
    }

    @Test
    void fixed_whenMemoryLimitIsNotPositive_shouldFail() {
        assertThatThrownBy(() -> S3CrtDirectBufferPoolConfiguration.fixed(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("memoryLimitInBytes");
        assertThatThrownBy(() -> S3CrtDirectBufferPoolConfiguration.fixed(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("memoryLimitInBytes");
    }

    @Test
    void elastic_whenSizesAreInvalid_shouldFail() {
        assertThatThrownBy(() -> S3CrtDirectBufferPoolConfiguration.elastic(-1, 1024))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("minimumSizeInBytes");
        assertThatThrownBy(() -> S3CrtDirectBufferPoolConfiguration.elastic(0, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("memoryLimitInBytes");
        assertThatThrownBy(() -> S3CrtDirectBufferPoolConfiguration.elastic(1025, 1024))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("minimumSizeInBytes");
    }
}
