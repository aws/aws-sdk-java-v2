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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperFieldModel.DynamoDbAttributeType;
import software.amazon.awssdk.services.dynamodb.datamodeling.StandardAnnotationMaps.TypedMap;
import software.amazon.awssdk.services.dynamodb.datamodeling.StandardTypeConverters.Scalar;
import software.amazon.awssdk.services.dynamodb.datamodeling.StandardTypeConverters.Vector;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Generic type helper.
 */
@SdkInternalApi
final class ConvertibleType<T> {

    private final DynamoDbTypeConverter<?, T> typeConverter;
    private final DynamoDbAttributeType attributeType;
    private final ConvertibleType<T>[] params;
    private final Class<T> targetType;

    @Deprecated
    private final Method getter;

    @Deprecated
    private final Method setter;

    /**
     * Constructs a new parameter type.
     */
    @SuppressWarnings("unchecked")
    private ConvertibleType(Type genericType, TypedMap<T> annotations, Method getter) {
        this.typeConverter = annotations.typeConverter();
        this.attributeType = annotations.attributeType();

        if (typeConverter != null) {
            final ConvertibleType<T> target = ConvertibleType.<T>of(typeConverter);
            this.targetType = target.targetType;
            this.params = target.params;
        } else if (genericType instanceof ParameterizedType) {
            final Type[] paramTypes = ((ParameterizedType) genericType).getActualTypeArguments();
            this.targetType = annotations.targetType();
            this.params = new ConvertibleType[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                this.params[i] = ConvertibleType.<T>of(paramTypes[i]);
            }
        } else {
            this.targetType = annotations.targetType();
            this.params = new ConvertibleType[0];
        }

        this.setter = getter == null ? null : StandardBeanProperties.MethodReflect.setterOf(getter);
        this.getter = getter;
    }

    /**
     * Returns the conversion type for the method and annotations.
     */
    static <T> ConvertibleType<T> of(Method getter, TypedMap<T> annotations) {
        return new ConvertibleType<T>(getter.getGenericReturnType(), annotations, getter);
    }

    /**
     * Returns the conversion type for the converter.
     */
    private static <T> ConvertibleType<T> of(final DynamoDbTypeConverter<?, T> converter) {
        final Class<?> clazz = converter.getClass();
        if (!clazz.isInterface()) {
            for (Class<?> c = clazz; Object.class != c; c = c.getSuperclass()) {
                for (final Type genericType : c.getGenericInterfaces()) {
                    final ConvertibleType<T> type = ConvertibleType.<T>of(genericType);
                    if (type.is(DynamoDbTypeConverter.class)) {
                        if (type.params.length == 2 && type.param(0).targetType() != Object.class) {
                            return type.param(0);
                        }
                    }
                }
            }
            final ConvertibleType<T> type = ConvertibleType.<T>of(clazz.getGenericSuperclass());
            if (type.is(DynamoDbTypeConverter.class)) {
                if (type.params.length > 0 && type.param(0).targetType() != Object.class) {
                    return type.param(0);
                }
            }
        }
        throw new DynamoDbMappingException("could not resolve type of " + clazz);
    }

    /**
     * Returns the conversion type for the generic type.
     */
    private static <T> ConvertibleType<T> of(Type genericType) {
        final Class<T> targetType;
        if (genericType instanceof Class) {
            targetType = (Class<T>) genericType;
        } else if (genericType instanceof ParameterizedType) {
            targetType = (Class<T>) ((ParameterizedType) genericType).getRawType();
        } else if (genericType.toString().equals("byte[]")) {
            targetType = (Class<T>) byte[].class;
        } else {
            targetType = (Class<T>) Object.class;
        }
        final TypedMap<T> annotations = StandardAnnotationMaps.<T>of(targetType);
        return new ConvertibleType<T>(genericType, annotations, null);
    }

    /**
     * Gets the target custom type-converter.
     */
    final <S> DynamoDbTypeConverter<S, T> typeConverter() {
        return (DynamoDbTypeConverter<S, T>) this.typeConverter;
    }

    /**
     * Gets the overriding attribute type.
     */
    final DynamoDbAttributeType attributeType() {
        return this.attributeType;
    }

    /**
     * Gets the getter method.
     */
    @Deprecated
    final Method getter() {
        return this.getter;
    }

    /**
     * Gets the setter method.
     */
    @Deprecated
    final Method setter() {
        return this.setter;
    }

    /**
     * Gets the scalar parameter types.
     */
    final <U> ConvertibleType<U> param(final int index) {
        return this.params.length > index ? (ConvertibleType<U>) this.params[index] : null;
    }

    /**
     * Returns true if the types match.
     */
    final boolean is(ScalarAttributeType scalarAttributeType, Vector vector) {
        return param(0) != null && param(0).is(scalarAttributeType) && is(vector);
    }

    /**
     * Returns true if the types match.
     */
    final boolean is(ScalarAttributeType scalarAttributeType) {
        return Scalar.of(targetType()).is(scalarAttributeType);
    }

    /**
     * Returns true if the types match.
     */
    final boolean is(Scalar scalar) {
        return scalar.is(targetType());
    }

    /**
     * Returns true if the types match.
     */
    final boolean is(Vector vector) {
        return vector.is(targetType());
    }

    /**
     * Returns true if the types match.
     */
    final boolean is(Class<?> type) {
        return type.isAssignableFrom(targetType());
    }

    /**
     * Gets the raw scalar type.
     */
    final Class<T> targetType() {
        return this.targetType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(targetType().getSimpleName());
        if (this.params.length > 0) {
            builder.append("<");
            for (int i = 0; i < this.params.length; i++) {
                builder.append(i == 0 ? "" : ",").append(this.params[i]);
            }
            builder.append(">");
        }
        return builder.toString();
    }

}
