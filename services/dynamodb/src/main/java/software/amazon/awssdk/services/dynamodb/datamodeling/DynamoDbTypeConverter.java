/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import software.amazon.awssdk.annotation.SdkInternalApi;

/**
 * Interface for converting types.
 *
 * @param <S> The DynamoDB standard type.
 * @param <T> The object's field/property type.
 */
public interface DynamoDbTypeConverter<S extends Object, T extends Object> {

    /**
     * Turns an object of type T into an object of type S.
     */
    S convert(T object);

    /**
     * Turns an object of type S into an object of type T.
     */
    T unconvert(S object);

    /**
     * An abstract converter with additional general purpose functions.
     */
    @SdkInternalApi
    abstract static class AbstractConverter<S, T> implements DynamoDbTypeConverter<S, T> {
        public static <S, U, T> ExtendedConverter<S, U, T> join(DynamoDbTypeConverter<S, U> source,
                                                                DynamoDbTypeConverter<U, T> target) {
            return new ExtendedConverter<S, U, T>(source, target);
        }

        public static <S, T> NullSafeConverter<S, T> nullSafe(DynamoDbTypeConverter<S, T> converter) {
            return new NullSafeConverter<S, T>(converter);
        }

        public <U> DynamoDbTypeConverter<S, U> joinAll(DynamoDbTypeConverter<T, U>... targets) {
            AbstractConverter<S, U> converter = (AbstractConverter<S, U>) nullSafe();
            for (DynamoDbTypeConverter<T, U> target : targets) {
                if (target != null) {
                    converter = converter.join((DynamoDbTypeConverter<U, U>) nullSafe(target));
                }
            }
            return converter;
        }

        public <U> ExtendedConverter<S, T, U> join(DynamoDbTypeConverter<T, U> target) {
            return AbstractConverter.<S, T, U>join(this, target);
        }

        public NullSafeConverter<S, T> nullSafe() {
            return AbstractConverter.<S, T>nullSafe(this);
        }
    }

    /**
     * A converter which wraps a source and target converter.
     */
    public static class ExtendedConverter<S, U, T> extends AbstractConverter<S, T> {
        private final DynamoDbTypeConverter<S, U> source;
        private final DynamoDbTypeConverter<U, T> target;

        public ExtendedConverter(DynamoDbTypeConverter<S, U> source, DynamoDbTypeConverter<U, T> target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public S convert(final T o) {
            U o1 = target.convert(o);
            S o2 = source.convert(o1);
            return o2;
            //return source.convert(target.convert(o));
        }

        @Override
        public T unconvert(final S o) {
            return target.unconvert(source.unconvert(o));
        }
    }

    /**
     * A general purpose delegating converter.
     */
    public static class DelegateConverter<S, T> extends AbstractConverter<S, T> {
        private final DynamoDbTypeConverter<S, T> delegate;

        public DelegateConverter(DynamoDbTypeConverter<S, T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public S convert(final T object) {
            return delegate.convert(object);
        }

        @Override
        public T unconvert(final S object) {
            return delegate.unconvert(object);
        }
    }

    /**
     * A converter which evaluates nullability before convert/unconvert.
     */
    public static class NullSafeConverter<S, T> extends DelegateConverter<S, T> {
        public NullSafeConverter(DynamoDbTypeConverter<S, T> delegate) {
            super(delegate);
        }

        @Override
        public S convert(final T object) {
            return object == null ? null : super.convert(object);
        }

        @Override
        public T unconvert(final S object) {
            return object == null ? null : super.unconvert(object);
        }
    }

}
