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

package software.amazon.awssdk.enhanced.dynamodb.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class SearchVectorUtilsTest {

    @Test
    public void toSearchVector_fromFloatArray() {
        List<AttributeValue> result = SearchVectorUtils.toSearchVector(new float[] {1.0f, 2.5f, -3.25f});

        assertThat(result).hasSize(3);
        assertThat(result.get(0).n()).isEqualTo("1.0");
        assertThat(result.get(1).n()).isEqualTo("2.5");
        assertThat(result.get(2).n()).isEqualTo("-3.25");
    }

    @Test
    public void toSearchVector_fromFloatList() {
        List<AttributeValue> result = SearchVectorUtils.toSearchVector(Arrays.asList(1.0f, 0.5f));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).n()).isEqualTo("1.0");
        assertThat(result.get(1).n()).isEqualTo("0.5");
    }

    @Test
    public void toSearchVector_nullFloatArray_returnsNull() {
        List<AttributeValue> result = SearchVectorUtils.toSearchVector((float[]) null);

        assertThat(result).isNull();
    }

    @Test
    public void toSearchVector_nullFloatList_returnsNull() {
        List<AttributeValue> result = SearchVectorUtils.toSearchVector((List<Float>) null);

        assertThat(result).isNull();
    }

    @Test
    public void toSearchVector_emptyFloatArray_returnsEmptyList() {
        List<AttributeValue> result = SearchVectorUtils.toSearchVector(new float[0]);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void toSearchVector_emptyFloatList_returnsEmptyList() {
        List<AttributeValue> result = SearchVectorUtils.toSearchVector(Collections.<Float>emptyList());

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void toSearchVector_nanInFloatArray_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> SearchVectorUtils.toSearchVector(new float[] {1.0f, Float.NaN}))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("NaN is not supported by the default converters.");
    }

    @Test
    public void toSearchVector_infinityInFloatArray_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> SearchVectorUtils.toSearchVector(new float[] {Float.POSITIVE_INFINITY}))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Infinite numbers are not supported by the default converters.");
    }

    @Test
    public void toSearchVector_nanInFloatList_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> SearchVectorUtils.toSearchVector(Arrays.asList(1.0f, Float.NaN)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("NaN is not supported by the default converters.");
    }

    @Test
    public void toSearchVector_infinityInFloatList_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> SearchVectorUtils.toSearchVector(Arrays.asList(Float.NEGATIVE_INFINITY)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Infinite numbers are not supported by the default converters.");
    }
}
