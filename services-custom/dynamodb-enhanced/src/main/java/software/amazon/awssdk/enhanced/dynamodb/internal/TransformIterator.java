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

import java.util.Iterator;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;

// TODO: Consider moving to SDK core
@SdkInternalApi
public class TransformIterator<T, R> implements Iterator<R> {
    private final Iterator<T> wrappedIterator;
    private final Function<T, R> transformFunction;

    private TransformIterator(Iterator<T> wrappedIterator, Function<T, R> transformFunction) {
        this.wrappedIterator = wrappedIterator;
        this.transformFunction = transformFunction;
    }

    public static <T, R> TransformIterator<T, R> create(Iterator<T> iterator, Function<T, R> transformFunction) {
        return new TransformIterator<>(iterator, transformFunction);
    }

    @Override
    public boolean hasNext() {
        return wrappedIterator.hasNext();
    }

    @Override
    public R next() {
        return transformFunction.apply(wrappedIterator.next());
    }
}
