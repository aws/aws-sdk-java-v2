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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
final class AnnotationUtils {

    private AnnotationUtils() {
    }

    /**
     * Expands annotation arrays to handle repeatable annotations by extracting individual
     * annotations from container annotations.
     */
    static List<Annotation> expandAnnotations(Annotation[]... annotationArrays) {
        if (annotationArrays == null || annotationArrays.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(annotationArrays)
                     .filter(annotations -> annotations != null && annotations.length > 0)
                     .flatMap(Arrays::stream)
                     .flatMap(AnnotationUtils::expandSingleAnnotation)
                     .collect(Collectors.toList());
    }

    private static Stream<Annotation> expandSingleAnnotation(Annotation annotation) {
        if (annotation == null) {
            return Stream.empty();
        }

        List<Annotation> containerAnnotations = extractFromContainer(annotation);
        return containerAnnotations.isEmpty()
               ? Stream.of(annotation)
               : containerAnnotations.stream();
    }

    private static List<Annotation> extractFromContainer(Annotation annotation) {
        try {
            Method valueMethod = annotation.annotationType().getDeclaredMethod("value");
            Class<?> returnType = valueMethod.getReturnType();

            if (!returnType.isArray() || !returnType.getComponentType().isAnnotation()) {
                return Collections.emptyList();
            }

            Annotation[] containedAnnotations = (Annotation[]) valueMethod.invoke(annotation);
            return containedAnnotations != null ? Arrays.asList(containedAnnotations) : Collections.emptyList();

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 SecurityException | ClassCastException e) {
            return Collections.emptyList();
        }
    }
}