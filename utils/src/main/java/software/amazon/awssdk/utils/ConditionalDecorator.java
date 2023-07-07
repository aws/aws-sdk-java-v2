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

import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.internal.DefaultConditionalDecorator;

/**
 * An interface that defines a class that contains a transform for another type as well as a condition for whether
 * that transform should be applied.
 *
 * @param <T> A type that can be decorated, or transformed, through applying a function.
 */
@FunctionalInterface
@SdkProtectedApi
public interface ConditionalDecorator<T> {

    default Predicate<T> predicate() {
        return t -> true;
    }


    UnaryOperator<T> transform();

    static <T> ConditionalDecorator<T> create(Predicate<T> predicate, UnaryOperator<T> transform) {
        DefaultConditionalDecorator.Builder<T> builder = new DefaultConditionalDecorator.Builder<>();
        return builder.predicate(predicate).transform(transform).build();
    }

    /**
     * This function will transform an initially supplied value with provided transforming, or decorating, functions that are
     * conditionally and sequentially applied. For each pair of condition and transform: if the condition evaluates to true, the
     * transform will be applied to the incoming value and the output from the transform is the input to the next transform.
     * <p>
     * If the supplied collection is ordered, the function is guaranteed to apply the transforms in the order in which they appear
     * in the collection.
     *
     * @param initialValue The untransformed start value
     * @param decorators   A list of condition to transform
     * @param <T>          The type of the value
     * @return A single transformed value that is the result of applying all transforms evaluated to true
     */
    static <T> T decorate(T initialValue, List<ConditionalDecorator<T>> decorators) {
        return decorators.stream()
                         .filter(d -> d.predicate().test(initialValue))
                         .reduce(initialValue,
                                 (element, decorator) -> decorator.transform().apply(element),
                                 (el1, el2) -> { throw new IllegalStateException("Should not reach here, combine function not "
                                                                                 + "needed unless executed in parallel."); });
    }
}
