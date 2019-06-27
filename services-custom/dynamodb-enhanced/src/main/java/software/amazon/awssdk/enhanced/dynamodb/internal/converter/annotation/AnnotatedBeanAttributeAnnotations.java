/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.converter.bean.annotation.AttributeElementType;
import software.amazon.awssdk.enhanced.dynamodb.converter.bean.annotation.AttributeName;
import software.amazon.awssdk.utils.Validate;

/**
 * A wrapper for {@link AttributeName} and {@link AttributeElementType}, that is capable of merging values from multiple
 * annotated methods.
 *
 * <p>
 * This is usually created with {@link #create(AnnotatedElement)} once for an attribute getter method and and again for a setter
 * method, and then these attributes are combined with {@link #mergeWith(AnnotatedBeanAttributeAnnotations)} so that they can
 * be used to govern the behavior of the {@link AnnotatedBeanAttribute}.
 */
@SdkInternalApi
public class AnnotatedBeanAttributeAnnotations {
    private final AttributeName attributeName;
    private final AttributeElementType attributeElementType;

    private AnnotatedBeanAttributeAnnotations(AttributeName attributeName,
                                              AttributeElementType attributeElementType) {
        this.attributeName = attributeName;
        this.attributeElementType = attributeElementType;
    }

    private AnnotatedBeanAttributeAnnotations(AnnotatedElement declaredAnnotations) {
        this.attributeName = declaredAnnotations.getAnnotation(AttributeName.class);
        this.attributeElementType = declaredAnnotations.getAnnotation(AttributeElementType.class);
    }

    public static AnnotatedBeanAttributeAnnotations create(AnnotatedElement declaredAnnotations) {
        return new AnnotatedBeanAttributeAnnotations(declaredAnnotations);
    }

    public Optional<AttributeName> attributeName() {
        return Optional.ofNullable(attributeName);
    }

    public Optional<AttributeElementType> attributeElementType() {
        return Optional.ofNullable(attributeElementType);
    }

    public AnnotatedBeanAttributeAnnotations mergeWith(AnnotatedBeanAttributeAnnotations rhs) {
        return new AnnotatedBeanAttributeAnnotations(mergeAttributeName(this, rhs),
                                                     mergeAttributeElementType(this, rhs));
    }

    private AttributeName mergeAttributeName(AnnotatedBeanAttributeAnnotations lhs,
                                             AnnotatedBeanAttributeAnnotations rhs) {
        return merge(lhs, rhs, a -> a.attributeName, (leftName, rightName) -> !leftName.value().equals(rightName.value()));
    }

    private AttributeElementType mergeAttributeElementType(AnnotatedBeanAttributeAnnotations lhs,
                                                           AnnotatedBeanAttributeAnnotations rhs) {
        return merge(lhs, rhs, a -> a.attributeElementType, (l, r) -> !Arrays.equals(l.value(), r.value()));
    }

    private <T extends Annotation> T merge(AnnotatedBeanAttributeAnnotations lhs,
                                           AnnotatedBeanAttributeAnnotations rhs,
                                           Function<AnnotatedBeanAttributeAnnotations, T> annotationExtractor,
                                           BiPredicate<T, T> isConflictingMerge) {
        T lhsAnnotation = annotationExtractor.apply(lhs);
        T rhsAnnotation = annotationExtractor.apply(rhs);
        if (lhsAnnotation == null) {
            return rhsAnnotation;
        } else {
            if (rhsAnnotation != null) {
                Validate.isTrue(!isConflictingMerge.test(lhsAnnotation, rhsAnnotation),
                                "Conflicting annotations on attribute: %s vs %s.",
                                lhsAnnotation, rhsAnnotation);
            }

            return lhsAnnotation;
        }
    }
}
