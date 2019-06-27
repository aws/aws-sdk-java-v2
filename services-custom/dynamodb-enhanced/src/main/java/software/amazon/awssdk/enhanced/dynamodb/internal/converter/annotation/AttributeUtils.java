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

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class AttributeUtils {
    private AttributeUtils() {}

    public static Optional<String> getMethodAttributeName(Method method) {
        String methodName = method.getName();

        if (methodName.length() <= 2) {
            return Optional.empty();
        }

        if (methodName.startsWith("is")) {
            return Optional.of(decapitalize(methodName.substring(2)));
        }

        if (methodName.length() <= 3) {
            return Optional.empty();
        }

        if (methodName.startsWith("get") || methodName.startsWith("set")) {
            return Optional.of(decapitalize(methodName.substring(3)));
        }

        return Optional.empty();
    }

    public static AnnotatedBeanAttribute.Type getAttributeMethodType(Method method) {
        String methodName = method.getName();

        if (methodName.startsWith("is") || methodName.startsWith("get")) {
            return AnnotatedBeanAttribute.Type.GETTER;
        } else if (methodName.startsWith("set")) {
            return AnnotatedBeanAttribute.Type.SETTER;
        }

        throw new IllegalArgumentException("Not a bean method: " + method);
    }


    private static String decapitalize(String string) {
        if (string.length() == 1) {
            return string.toLowerCase(Locale.US);
        }

        return string.substring(0, 1).toLowerCase(Locale.US) + string.substring(1);
    }
}
