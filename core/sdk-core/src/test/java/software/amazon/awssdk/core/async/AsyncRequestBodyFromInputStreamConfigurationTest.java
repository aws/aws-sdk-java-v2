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

package software.amazon.awssdk.core.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class AsyncRequestBodyFromInputStreamConfigurationTest {

    @Test
    void equalsHashcode() {
        EqualsVerifier.forClass(AsyncRequestBodyFromInputStreamConfiguration.class)
                      .verify();
    }

    @Test
    void toBuilder_shouldCopyProperties() {
        InputStream inputStream = mock(InputStream.class);
        ExecutorService executorService = mock(ExecutorService.class);
        AsyncRequestBodyFromInputStreamConfiguration configuration = AsyncRequestBodyFromInputStreamConfiguration.builder()
                                                                                                                 .inputStream(inputStream)
                                                                                                                 .contentLength(10L)
                                                                                                                 .executor(executorService)
                                                                                                                 .maxReadLimit(10)
                                                                                                                 .build();
        assertThat(configuration.toBuilder().build()).isEqualTo(configuration);

    }

    @Test
    void inputStreamIsNull_shouldThrowException() {
        assertThatThrownBy(() ->
                               AsyncRequestBodyFromInputStreamConfiguration.builder()
                                                                           .executor(mock(ExecutorService.class))
                                                                           .build())
            .isInstanceOf(NullPointerException.class).hasMessageContaining("inputStream");
    }


    @Test
    void executorIsNull_shouldThrowException() {
        assertThatThrownBy(() ->
                               AsyncRequestBodyFromInputStreamConfiguration.builder()
                                                                           .inputStream(mock(InputStream.class))
                                                                           .build())
            .isInstanceOf(NullPointerException.class).hasMessageContaining("executor");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void readLimitNotPositive_shouldThrowException(int value) {
        assertThatThrownBy(() ->
                               AsyncRequestBodyFromInputStreamConfiguration.builder()
                                                                           .inputStream(mock(InputStream.class))
                                                                           .executor(mock(ExecutorService.class))
                                                                           .maxReadLimit(value)
                                                                           .build())
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("maxReadLimit");
    }

    @Test
    void contentLengthNegative_shouldThrowException() {
        assertThatThrownBy(() ->
                               AsyncRequestBodyFromInputStreamConfiguration.builder()
                                                                           .inputStream(mock(InputStream.class))
                                                                           .executor(mock(ExecutorService.class))
                                                                           .contentLength(-1L)
                                                                           .build())
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("contentLength");
    }
}
