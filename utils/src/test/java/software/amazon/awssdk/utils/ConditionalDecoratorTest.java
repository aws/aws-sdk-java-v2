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

package software.amazon.awssdk.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class ConditionalDecoratorTest {

    @Test
    void basicTransform_directlyCalled_isSuccessful() {
        ConditionalDecorator<Integer> decorator = ConditionalDecorator.create(i -> true, i -> i + 1);
        assertThat(decorator.transform().apply(3)).isEqualTo(4);
    }

    @Test
    void listOfOrderedTransforms_singleTransformAlwaysTrue_isSuccessful() {
        ConditionalDecorator<Integer> d1 = ConditionalDecorator.create(i -> true, i -> i + 1);
        assertThat(ConditionalDecorator.decorate(2, Collections.singletonList(d1))).isEqualTo(3);
    }

    @Test
    void listOfOrderedTransforms_alwaysTrue_isSuccessful() {
        ConditionalDecorator<Integer> d1 = ConditionalDecorator.create(i -> true, i -> i + 1);
        ConditionalDecorator<Integer> d2 = ConditionalDecorator.create(i -> true, i -> i * 2);
        assertThat(ConditionalDecorator.decorate(2, Arrays.asList(d1, d2))).isEqualTo(6);
    }

    @Test
    void listOfOrderedTransformsInReverse_alwaysTrue_isSuccessful() {
        ConditionalDecorator<Integer> d1 = ConditionalDecorator.create(i -> true, i -> i + 1);
        ConditionalDecorator<Integer> d2 = ConditionalDecorator.create(i -> true, i -> i * 2);
        assertThat(ConditionalDecorator.decorate(2, Arrays.asList(d2, d1))).isEqualTo(5);
    }

    @Test
    void listOfOrderedTransforms_onlyAddsEvenNumbers_isSuccessful() {
        List<ConditionalDecorator<Integer>> decorators =
            IntStream.range(0, 9)
                     .<ConditionalDecorator<Integer>>mapToObj(i -> ConditionalDecorator.create(j -> i % 2 == 0,
                                                                                               j -> j + i))
                     .collect(Collectors.toList());
        assertThat(ConditionalDecorator.decorate(0, decorators)).isEqualTo(20);
    }
}
