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

package software.amazon.awssdk.utils.internal;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.ConditionalDecorator;

@SdkInternalApi
public final class DefaultConditionalDecorator<T> implements ConditionalDecorator<T> {
    private final Predicate<T> predicate;
    private final UnaryOperator<T> transform;

    DefaultConditionalDecorator(Builder<T> builder) {
        this.predicate = builder.predicate;
        this.transform = builder.transform;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Override
    public Predicate<T> predicate() {
        return predicate;
    }

    @Override
    public UnaryOperator<T> transform() {
        return transform;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultConditionalDecorator)) {
            return false;
        }

        DefaultConditionalDecorator<?> that = (DefaultConditionalDecorator<?>) o;

        if (!Objects.equals(predicate, that.predicate)) {
            return false;
        }
        return Objects.equals(transform, that.transform);
    }

    @Override
    public int hashCode() {
        int result = predicate != null ? predicate.hashCode() : 0;
        result = 31 * result + (transform != null ? transform.hashCode() : 0);
        return result;
    }

    public static final class Builder<T> {
        private Predicate<T> predicate;
        private UnaryOperator<T> transform;

        public Builder<T> predicate(Predicate<T> predicate) {
            this.predicate = predicate;
            return this;
        }

        public Builder<T> transform(UnaryOperator<T> transform) {
            this.transform = transform;
            return this;
        }

        public ConditionalDecorator<T> build() {
            return new DefaultConditionalDecorator<>(this);
        }
    }
}
