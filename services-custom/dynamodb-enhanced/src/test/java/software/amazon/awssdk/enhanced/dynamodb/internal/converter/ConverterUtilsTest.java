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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConverterUtilsTest {

    @Test
    void validateDouble_nullDoesNotThrow() {
        assertThatCode(() -> ConverterUtils.validateDouble(null))
            .doesNotThrow();
    }

    @Test
    void validateDouble_finiteValueDoesNotThrow() {
        assertThatCode(() -> ConverterUtils.validateDouble(3.14))
            .doesNotThrow();
    }

    @Test
    void validateDouble_nanThrows() {
        assertThatThrownBy(() -> ConverterUtils.validateDouble(Double.NaN))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("NaN");
    }

    @Test
    void validateDouble_infiniteThrows() {
        assertThatThrownBy(() -> ConverterUtils.validateDouble(Double.POSITIVE_INFINITY))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Infinite");
    }

    @Test
    void validateFloat_nullDoesNotThrow() {
        assertThatCode(() -> ConverterUtils.validateFloat(null))
            .doesNotThrow();
    }

    @Test
    void validateFloat_nanThrows() {
        assertThatThrownBy(() -> ConverterUtils.validateFloat(Float.NaN))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("NaN");
    }
}
