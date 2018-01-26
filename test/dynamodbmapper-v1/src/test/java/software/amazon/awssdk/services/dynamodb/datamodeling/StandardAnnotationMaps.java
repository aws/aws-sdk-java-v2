/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbAutoGenerateStrategy.CREATE;
import static software.amazon.awssdk.services.dynamodb.model.KeyType.HASH;
import static software.amazon.awssdk.services.dynamodb.model.KeyType.RANGE;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperFieldModel.DynamoDbAttributeType;
import software.amazon.awssdk.services.dynamodb.model.KeyType;

/**
 * Map of DynamoDB annotations.
 */
@SdkInternalApi
final class StandardAnnotationMaps {

    /**
     * Gets all the DynamoDB annotations for a given class.
     */
    static <T> TableMap<T> of(Class<T> clazz) {
        final TableMap<T> annotations = new TableMap<T>(clazz);
        annotations.putAll(clazz);
        return annotations;
    }

    /**
     * Gets all the DynamoDB annotations; method annotations override field
     * level annotations which override class/type level annotations.
     */
    static <T> FieldMap<T> of(Method getter, String defaultName) {
        final Class<T> targetType = (Class<T>) getter.getReturnType();
        final String fieldName = StandardBeanProperties.fieldNameOf(getter);

        Field declaredField = null;
        try {
            declaredField = getter.getDeclaringClass().getDeclaredField(fieldName);
        } catch (final NoSuchFieldException no) {
            // Ignored or expected.
        } catch (final SecurityException e) {
            throw new DynamoDbMappingException("no access to field for " + getter, e);
        }

        if (defaultName == null) {
            defaultName = fieldName;
        }

        final FieldMap<T> annotations = new FieldMap<T>(targetType, defaultName);
        annotations.putAll(targetType);
        annotations.putAll(declaredField);
        annotations.putAll(getter);
        return annotations;
    }

    /**
     * Creates a new instance of the clazz with the target type and annotation
     * as parameters if available.
     */
    private static <T> T overrideOf(Class<T> clazz, Class<?> targetType, Annotation annotation) {
        try {
            if (annotation != null) {
                try {
                    Constructor<T> c = clazz.getDeclaredConstructor(Class.class, annotation.annotationType());
                    return c.newInstance(targetType, annotation);
                } catch (final NoSuchMethodException no) {
                    // Ignored or expected.
                }
            }
            try {
                return clazz.getDeclaredConstructor(Class.class).newInstance(targetType);
            } catch (final NoSuchMethodException no) {
                // Ignored or expected.
            }
            return clazz.newInstance();
        } catch (final IllegalAccessException | InstantiationException | InvocationTargetException | RuntimeException e) {
            throw new DynamoDbMappingException("could not instantiate " + clazz, e);
        }
    }

    /**
     * Common type-conversions properties.
     */
    private abstract static class AbstractAnnotationMap {
        private final Annotations map = new Annotations();

        /**
         * Gets the actual annotation by type; if the type is not directly
         * mapped then the meta-annotation is returned.
         */
        final <A extends Annotation> A actualOf(final Class<A> annotationType) {
            final Annotation annotation = this.map.get(annotationType);
            if (annotation == null || annotation.annotationType() == annotationType) {
                return (A) annotation;
            } else if (annotation.annotationType().isAnnotationPresent(annotationType)) {
                return annotation.annotationType().getAnnotation(annotationType);
            }
            throw new DynamoDbMappingException(
                    "could not resolve annotation by type" +
                    "; @" + annotationType.getSimpleName() + " not present on " + annotation
            );
        }

        /**
         * Puts all DynamoDB annotations into the map.
         */
        final void putAll(AnnotatedElement annotated) {
            if (annotated != null) {
                this.map.putAll(new Annotations().putAll(annotated.getAnnotations()));
            }
        }
    }

    /**
     * Common type-conversions properties.
     */
    abstract static class TypedMap<T> extends AbstractAnnotationMap {
        private final Class<T> targetType;

        private TypedMap(final Class<T> targetType) {
            this.targetType = targetType;
        }

        /**
         * Gets the target type.
         */
        final Class<T> targetType() {
            return this.targetType;
        }

        /**
         * Gets the attribute type from the {@link DynamoDbTyped} annotation
         * if present.
         */
        public DynamoDbAttributeType attributeType() {
            final DynamoDbTyped annotation = actualOf(DynamoDbTyped.class);
            if (annotation != null) {
                return annotation.value();
            }
            return null;
        }

        /**
         * Creates a new type-converter form the {@link DynamoDbTypeConverted}
         * annotation if present.
         */
        public <S> DynamoDbTypeConverter<S, T> typeConverter() {
            Annotation annotation = super.map.get(DynamoDbTypeConverted.class);
            if (annotation != null) {
                final DynamoDbTypeConverted converted = actualOf(DynamoDbTypeConverted.class);
                annotation = (converted == annotation ? null : annotation);
                return overrideOf(converted.converter(), targetType, annotation);
            }
            return null;
        }

        /**
         * Creates a new auto-generator from the {@link DynamoDbAutoGenerated}
         * annotation if present.
         */
        public DynamoDbAutoGenerator<T> autoGenerator() {
            Annotation annotation = super.map.get(DynamoDbAutoGenerated.class);
            if (annotation != null) {
                final DynamoDbAutoGenerated generated = actualOf(DynamoDbAutoGenerated.class);
                annotation = (generated == annotation ? null : annotation);
                DynamoDbAutoGenerator<T> generator = overrideOf(generated.generator(), targetType, annotation);
                if (generator.getGenerateStrategy() == CREATE && targetType.isPrimitive()) {
                    throw new DynamoDbMappingException(
                            "type [" + targetType + "] is not supported for auto-generation" +
                            "; primitives are not allowed when auto-generate strategy is CREATE"
                    );
                }
                return generator;
            }
            return null;
        }

        /**
         * Maps the attributes from the {@link DynamoDbFlattened} annotation.
         */
        public Map<String, String> attributes() {
            final Map<String, String> attributes = new LinkedHashMap<String, String>();
            for (final DynamoDbAttribute a : actualOf(DynamoDbFlattened.class).attributes()) {
                if (a.mappedBy().isEmpty() || a.attributeName().isEmpty()) {
                    throw new DynamoDbMappingException("@DynamoDBFlattened must specify mappedBy and attributeName");
                } else if (attributes.put(a.mappedBy(), a.attributeName()) != null) {
                    throw new DynamoDbMappingException("@DynamoDBFlattened must not duplicate mappedBy=" + a.mappedBy());
                }
            }
            if (attributes.isEmpty()) {
                throw new DynamoDbMappingException("@DynamoDBFlattened must specify one or more attributes");
            }
            return attributes;
        }

        /**
         * Returns true if the {@link DynamoDbFlattened} annotation is present.
         */
        public boolean flattened() {
            return actualOf(DynamoDbFlattened.class) != null;
        }
    }

    /**
     * {@link DynamoDbMapperTableModel} annotations.
     */
    static final class TableMap<T> extends TypedMap<T> implements DynamoDbMapperTableModel.Properties<T> {
        private TableMap(final Class<T> targetType) {
            super(targetType);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DynamoDbAttributeType attributeType() {
            DynamoDbAttributeType attributeType = super.attributeType();
            if (attributeType == null && actualOf(DynamoDbTable.class) != null) {
                attributeType = DynamoDbAttributeType.M;
            }
            return attributeType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String tableName() {
            final DynamoDbTable annotation = actualOf(DynamoDbTable.class);
            if (annotation != null && !annotation.tableName().isEmpty()) {
                return annotation.tableName();
            }
            return null;
        }
    }

    /**
     * {@link DynamoDbMapperFieldModel} annotations.
     */
    static final class FieldMap<T> extends TypedMap<T> implements DynamoDbMapperFieldModel.Properties<T> {
        private final String defaultName;

        private FieldMap(Class<T> targetType, String defaultName) {
            super(targetType);
            this.defaultName = defaultName;
        }

        /**
         * Returns true if the {@link DynamoDbIgnore} annotation is present.
         */
        public boolean ignored() {
            return actualOf(DynamoDbIgnore.class) != null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DynamoDbAttributeType attributeType() {
            final DynamoDbScalarAttribute annotation = actualOf(DynamoDbScalarAttribute.class);
            if (annotation != null) {
                if (Set.class.isAssignableFrom(targetType())) {
                    return DynamoDbAttributeType.valueOf(annotation.type().name() + "S");
                } else {
                    return DynamoDbAttributeType.valueOf(annotation.type().name());
                }
            }
            return super.attributeType();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String attributeName() {
            final DynamoDbHashKey hashKey = actualOf(DynamoDbHashKey.class);
            if (hashKey != null && !hashKey.attributeName().isEmpty()) {
                return hashKey.attributeName();
            }
            final DynamoDbIndexHashKey indexHashKey = actualOf(DynamoDbIndexHashKey.class);
            if (indexHashKey != null && !indexHashKey.attributeName().isEmpty()) {
                return indexHashKey.attributeName();
            }
            final DynamoDbRangeKey rangeKey = actualOf(DynamoDbRangeKey.class);
            if (rangeKey != null && !rangeKey.attributeName().isEmpty()) {
                return rangeKey.attributeName();
            }
            final DynamoDbIndexRangeKey indexRangeKey = actualOf(DynamoDbIndexRangeKey.class);
            if (indexRangeKey != null && !indexRangeKey.attributeName().isEmpty()) {
                return indexRangeKey.attributeName();
            }
            final DynamoDbAttribute attribute = actualOf(DynamoDbAttribute.class);
            if (attribute != null && !attribute.attributeName().isEmpty()) {
                return attribute.attributeName();
            }
            final DynamoDbVersionAttribute versionAttribute = actualOf(DynamoDbVersionAttribute.class);
            if (versionAttribute != null && !versionAttribute.attributeName().isEmpty()) {
                return versionAttribute.attributeName();
            }
            final DynamoDbScalarAttribute scalarAttribute = actualOf(DynamoDbScalarAttribute.class);
            if (scalarAttribute != null && !scalarAttribute.attributeName().isEmpty()) {
                return scalarAttribute.attributeName();
            }
            final DynamoDbNamed annotation = actualOf(DynamoDbNamed.class);
            if (annotation != null && !annotation.value().isEmpty()) {
                return annotation.value();
            }
            return this.defaultName;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public KeyType keyType() {
            final DynamoDbKeyed annotation = actualOf(DynamoDbKeyed.class);
            if (annotation != null) {
                return annotation.value();
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean versioned() {
            return actualOf(DynamoDbVersioned.class) != null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Map<KeyType, List<String>> globalSecondaryIndexNames() {
            final Map<KeyType, List<String>> gsis = new EnumMap<KeyType, List<String>>(KeyType.class);
            final DynamoDbIndexHashKey indexHashKey = actualOf(DynamoDbIndexHashKey.class);
            if (indexHashKey != null) {
                if (!indexHashKey.globalSecondaryIndexName().isEmpty()) {
                    if (indexHashKey.globalSecondaryIndexNames().length > 0) {
                        throw new DynamoDbMappingException("@DynamoDBIndexHashKey must not specify both HASH GSI name/names");
                    }
                    gsis.put(HASH, Collections.singletonList(indexHashKey.globalSecondaryIndexName()));
                } else if (indexHashKey.globalSecondaryIndexNames().length > 0) {
                    gsis.put(HASH, Collections.unmodifiableList(Arrays.asList(indexHashKey.globalSecondaryIndexNames())));
                } else {
                    throw new DynamoDbMappingException("@DynamoDBIndexHashKey must specify one of HASH GSI name/names");
                }
            }
            final DynamoDbIndexRangeKey indexRangeKey = actualOf(DynamoDbIndexRangeKey.class);
            if (indexRangeKey != null) {
                if (!indexRangeKey.globalSecondaryIndexName().isEmpty()) {
                    if (indexRangeKey.globalSecondaryIndexNames().length > 0) {
                        throw new DynamoDbMappingException("@DynamoDBIndexRangeKey must not specify both RANGE GSI name/names");
                    }
                    gsis.put(RANGE, Collections.singletonList(indexRangeKey.globalSecondaryIndexName()));
                } else if (indexRangeKey.globalSecondaryIndexNames().length > 0) {
                    gsis.put(RANGE, Collections.unmodifiableList(Arrays.asList(indexRangeKey.globalSecondaryIndexNames())));
                } else if (localSecondaryIndexNames().isEmpty()) {
                    throw new DynamoDbMappingException("@DynamoDBIndexRangeKey must specify RANGE GSI and/or LSI name/names");
                }
            }
            if (!gsis.isEmpty()) {
                return Collections.unmodifiableMap(gsis);
            }
            return Collections.<KeyType, List<String>>emptyMap();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> localSecondaryIndexNames() {
            final DynamoDbIndexRangeKey annotation = actualOf(DynamoDbIndexRangeKey.class);
            if (annotation != null) {
                if (!annotation.localSecondaryIndexName().isEmpty()) {
                    if (annotation.localSecondaryIndexNames().length > 0) {
                        throw new DynamoDbMappingException("@DynamoDBIndexRangeKey must not specify both LSI name/names");
                    }
                    return Collections.singletonList(annotation.localSecondaryIndexName());
                } else if (annotation.localSecondaryIndexNames().length > 0) {
                    return Collections.unmodifiableList(Arrays.asList(annotation.localSecondaryIndexNames()));
                }
            }
            return Collections.<String>emptyList();
        }
    }

    /**
     * A map of annotation type to annotation. It will map any first level
     * custom annotations to any DynamoDB annotation types that are present.
     * It will support up to two levels of compounded DynamoDB annotations.
     */
    private static final class Annotations extends LinkedHashMap<Class<? extends Annotation>, Annotation> {
        private static final long serialVersionUID = -1L;

        /**
         * Puts the annotation if it's DynamoDB; ensures there are no conflicts.
         */
        public boolean putIfAnnotated(Class<? extends Annotation> annotationType, Annotation annotation) {
            if (!annotationType.isAnnotationPresent(DynamoDb.class)) {
                return false;
            } else {
                annotation = put(annotationType, annotation);
                if (annotation == null) {
                    return true;
                }
            }
            throw new DynamoDbMappingException(
                    "conflicting annotations " + annotation + " and " + get(annotationType) +
                    "; allowed only one of @" + annotationType.getSimpleName()
            );
        }

        /**
         * Puts all DynamoDB annotations and meta-annotations in the map.
         */
        public Annotations putAll(Annotation... annotations) {
            for (final Annotation a1 : annotations) {
                putIfAnnotated(a1.annotationType(), a1);
                for (final Annotation a2 : a1.annotationType().getAnnotations()) {
                    if (putIfAnnotated(a2.annotationType(), a1)) {
                        for (final Annotation a3 : a2.annotationType().getAnnotations()) {
                            putIfAnnotated(a3.annotationType(), a2);
                        }
                    }
                }
            }
            return this;
        }
    }

}
