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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ConverterUtilsTest {

    @Test
    void validateDouble_withNull_doesNotThrowException() {
        assertThatCode(() -> ConverterUtils.validateDouble(null))
            .doesNotThrowAnyException();
    }

    @Test
    void validateDouble_withValidValue_doesNotThrowException() {
        assertThatCode(() -> ConverterUtils.validateDouble(1.5))
            .doesNotThrowAnyException();
    }

    @Test
    void validateDouble_withNaN_throwsException() {
        assertThatThrownBy(() -> ConverterUtils.validateDouble(Double.NaN))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("NaN is not supported");
    }

    @Test
    void validateDouble_withPositiveInfinity_throwsException() {
        assertThatThrownBy(() -> ConverterUtils.validateDouble(Double.POSITIVE_INFINITY))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Infinite numbers are not supported");
    }

    @Test
    void validateDouble_withNegativeInfinity_throwsException() {
        assertThatThrownBy(() -> ConverterUtils.validateDouble(Double.NEGATIVE_INFINITY))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Infinite numbers are not supported");
    }

    @Test
    void validateFloat_withNull_doesNotThrowException() {
        assertThatCode(() -> ConverterUtils.validateFloat(null))
            .doesNotThrowAnyException();
    }

    @Test
    void validateFloat_withValidValue_doesNotThrowException() {
        assertThatCode(() -> ConverterUtils.validateFloat(1.5f))
            .doesNotThrowAnyException();
    }

    @Test
    void validateFloat_withNaN_throwsException() {
        assertThatThrownBy(() -> ConverterUtils.validateFloat(Float.NaN))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("NaN is not supported");
    }

    @Test
    void validateFloat_withPositiveInfinity_throwsException() {
        assertThatThrownBy(() -> ConverterUtils.validateFloat(Float.POSITIVE_INFINITY))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Infinite numbers are not supported");
    }

    @Test
    void validateFloat_withNegativeInfinity_throwsException() {
        assertThatThrownBy(() -> ConverterUtils.validateFloat(Float.NEGATIVE_INFINITY))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Infinite numbers are not supported");
    }
}
