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

package software.amazon.awssdk.enhanced.dynamodb.internal.immutable;

import java.beans.Transient;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;

@SdkInternalApi
public class ImmutableIntrospector {
    private static final String BUILD_METHOD = "build";
    private static final String BUILDER_METHOD = "builder";
    private static final String TO_BUILDER_METHOD = "toBuilder";
    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";
    private static final String SET_PREFIX = "set";

    private static final String[] METHODS_TO_IGNORE = { TO_BUILDER_METHOD };

    private static volatile ImmutableIntrospector INSTANCE = null;

    // Methods from Object are commonly overridden and confuse the mapper, automatically exclude any method with a name
    // that matches a method defined on Object.
    private final Set<String> namesToExclude;

    private ImmutableIntrospector() {
        this.namesToExclude = Collections.unmodifiableSet(
            Stream.concat(Arrays.stream(Object.class.getMethods()).map(Method::getName), Arrays.stream(METHODS_TO_IGNORE))
                  .collect(Collectors.toSet()));
    }

    public static <T> ImmutableInfo<T> getImmutableInfo(Class<T> immutableClass) {
        if (INSTANCE == null) {
            synchronized (ImmutableIntrospector.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ImmutableIntrospector();
                }
            }
        }

        return INSTANCE.introspect(immutableClass);
    }

    private <T> ImmutableInfo<T> introspect(Class<T> immutableClass) {
        Class<?> builderClass = validateAndGetBuilderClass(immutableClass);
        Optional<Method> staticBuilderMethod = findStaticBuilderMethod(immutableClass, builderClass);
        List<Method> getters = filterAndCollectGetterMethods(immutableClass.getMethods());
        Map<String, Method> indexedBuilderMethods = filterAndIndexBuilderMethods(builderClass.getMethods());
        Method buildMethod = extractBuildMethod(indexedBuilderMethods, immutableClass)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "An immutable builder class must have a public method named 'build()' that takes no arguments " +
                        "and returns an instance of the immutable class it builds"));

        List<ImmutablePropertyDescriptor> propertyDescriptors =
            getters.stream()
                   .map(getter -> {
                       validateGetter(getter);
                       return resolvePropertyDescriptor(getter, indexedBuilderMethods, builderClass);
                   }).collect(Collectors.toList());

        if (!indexedBuilderMethods.isEmpty()) {
            throw generateExceptionForMethod(indexedBuilderMethods.values().iterator().next(),
                                             "A method was found on the immutable class builder that does not appear " +
                                                 "to have a matching getter on the immutable class.");
        }

        return ImmutableInfo.builder(immutableClass)
                            .builderClass(builderClass)
                            .staticBuilderMethod(staticBuilderMethod.orElse(null))
                            .buildMethod(buildMethod)
                            .propertyDescriptors(propertyDescriptors)
                            .build();
    }

    private boolean isMappableMethod(Method method) {
        return method.getDeclaringClass() != Object.class
            && method.getAnnotation(DynamoDbIgnore.class) == null
            && method.getAnnotation(Transient.class) == null
            && !method.isSynthetic()
            && !method.isBridge()
            && !Modifier.isStatic(method.getModifiers())
            && !namesToExclude.contains(method.getName());
    }

    private Optional<Method> findStaticBuilderMethod(Class<?> immutableClass, Class<?> builderClass) {
        try {
            Method method = immutableClass.getMethod(BUILDER_METHOD);

            if (Modifier.isStatic(method.getModifiers()) && method.getReturnType().isAssignableFrom(builderClass)) {
                return Optional.of(method);
            }
        } catch (NoSuchMethodException ignored) {
            // no-op
        }

        return Optional.empty();
    }

    private IllegalArgumentException generateExceptionForMethod(Method getter, String message) {
        return new IllegalArgumentException(
            message + " Use the @DynamoDbIgnore annotation on the method if you do not want it to be included in the " +
                "TableSchema introspection. [Method = \"" + getter + "\"]");
    }

    private Class<?> validateAndGetBuilderClass(Class<?> immutableClass) {
        DynamoDbImmutable dynamoDbImmutable = immutableClass.getAnnotation(DynamoDbImmutable.class);

        if (dynamoDbImmutable == null) {
            throw new IllegalArgumentException("A DynamoDb immutable class must be annotated with @DynamoDbImmutable");
        }

        return dynamoDbImmutable.builder();
    }

    private void validateGetter(Method getter) {
        if (getter.getReturnType() == void.class || getter.getReturnType() == Void.class) {
            throw generateExceptionForMethod(getter, "A method was found on the immutable class that does not appear " +
                "to be a valid getter due to the return type being void.");
        }

        if (getter.getParameterCount() != 0) {
            throw generateExceptionForMethod(getter, "A method was found on the immutable class that does not appear " +
                "to be a valid getter due to it having one or more parameters.");
        }
    }

    private List<Method> filterAndCollectGetterMethods(Method[] rawMethods) {
        return Arrays.stream(rawMethods)
                     .filter(this::isMappableMethod)
                     .collect(Collectors.toList());
    }

    private Map<String, Method> filterAndIndexBuilderMethods(Method[] rawMethods) {
        return Arrays.stream(rawMethods)
                     .filter(this::isMappableMethod)
                     .collect(Collectors.toMap(this::normalizeSetterName, m -> m));
    }

    private String normalizeSetterName(Method setter) {
        String setterName = setter.getName();

        if (setterName.length() > 3
            && Character.isUpperCase(setterName.charAt(3))
            && setterName.startsWith(SET_PREFIX)) {

            return Character.toLowerCase(setterName.charAt(3)) + setterName.substring(4);
        }

        return setterName;
    }

    private String normalizeGetterName(Method getter) {
        String getterName = getter.getName();

        if (getterName.length() > 2
            && Character.isUpperCase(getterName.charAt(2))
            && getterName.startsWith(IS_PREFIX)
            && isMethodBoolean(getter)) {

            return Character.toLowerCase(getterName.charAt(2)) + getterName.substring(3);
        }

        if (getterName.length() > 3
            && getterName.startsWith(GET_PREFIX)
            && isGetPrefixRemainderStart(getterName.charAt(3))) {

            return Character.toLowerCase(getterName.charAt(3)) + getterName.substring(4);
        }

        return getterName;
    }

    /**
     * Returns true when the character immediately after a {@code get} prefix should trigger prefix stripping.
     * Uppercase letters preserve existing stripping (for example {@code getId} to {@code id}).
     * Underscore and dollar are also accepted so getters such as {@code get__encryption_keyId} and
     * {@code get$encryptionKey} normalize to fluent builder names like {@code __encryption_keyId} /
     * {@code $encryptionKey} (see issue #5589).
     */
    private static boolean isGetPrefixRemainderStart(char remainderStart) {
        return Character.isUpperCase(remainderStart) || remainderStart == '_' || remainderStart == '$';
    }

    /**
     * Resolves the immutable property name and matching builder method for a getter.
     *
     * <p>Prefers the normalized property name when a builder method with a valid signature exists for that name.
     * For {@code get_}/{@code get$} getters only, if no valid normalized builder method exists, falls back to the
     * getter's original method name so models that previously paired {@code get__encryption_keyId()} with a builder
     * method also named {@code get__encryption_keyId(...)} continue to work. Ordinary {@code getX}/{@code isX}
     * normalization is unchanged and does not use this fallback. A builder method that only matches by name but has
     * an invalid signature is not treated as a match.
     */
    private ImmutablePropertyDescriptor resolvePropertyDescriptor(Method getter,
                                                                  Map<String, Method> indexedBuilderMethods,
                                                                  Class<?> builderClass) {
        String normalizedName = normalizeGetterName(getter);
        Optional<Method> setter = extractSetterMethod(normalizedName, indexedBuilderMethods, getter, builderClass);
        String propertyName = normalizedName;

        if (!setter.isPresent() && shouldAttemptLegacyGetterNameFallback(getter, normalizedName)) {
            Optional<Method> legacySetter =
                extractSetterMethod(getter.getName(), indexedBuilderMethods, getter, builderClass);
            if (legacySetter.isPresent()) {
                setter = legacySetter;
                propertyName = getter.getName();
            }
        }

        Method resolvedSetter = setter.orElseThrow(
            () -> generateExceptionForMethod(
                getter,
                "A method was found on the immutable class that does not appear to have a " +
                    "matching setter on the builder class."));

        return ImmutablePropertyDescriptor.create(propertyName, getter, resolvedSetter);
    }

    /**
     * Legacy fallback applies only when the new {@code _}/{@code $} get-prefix stripping changed the property key.
     * It must not apply to ordinary {@code getId}/{@code isX} normalization, which historically never used the
     * raw getter name as the lookup key after a successful strip.
     */
    private static boolean shouldAttemptLegacyGetterNameFallback(Method getter, String normalizedName) {
        String getterName = getter.getName();
        if (normalizedName.equals(getterName) || getterName.length() <= 3 || !getterName.startsWith(GET_PREFIX)) {
            return false;
        }

        char remainderStart = getterName.charAt(3);
        return remainderStart == '_' || remainderStart == '$';
    }

    private boolean isMethodBoolean(Method method) {
        return method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class;
    }

    private Optional<Method> extractBuildMethod(Map<String, Method> indexedBuilderMethods, Class<?> immutableClass) {
        Method buildMethod = indexedBuilderMethods.get(BUILD_METHOD);

        if (buildMethod == null
            || buildMethod.getParameterCount() != 0
            || !immutableClass.equals(buildMethod.getReturnType())) {

            return Optional.empty();
        }

        indexedBuilderMethods.remove(BUILD_METHOD);
        return Optional.of(buildMethod);
    }

    private Optional<Method> extractSetterMethod(String propertyName,
                                                 Map<String, Method> indexedBuilderMethods,
                                                 Method getterMethod,
                                                 Class<?> builderClass) {
        Method setterMethod = indexedBuilderMethods.get(propertyName);

        if (setterMethod == null
            || !setterHasValidSignature(setterMethod, getterMethod.getReturnType(), builderClass)) {
            return Optional.empty();
        }

        indexedBuilderMethods.remove(propertyName);
        return Optional.of(setterMethod);
    }

    private boolean setterHasValidSignature(Method setterMethod, Class<?> expectedType, Class<?> builderClass) {
        return setterHasValidParameterSignature(setterMethod, expectedType)
            && setterHasValidReturnType(setterMethod, builderClass);
    }

    private boolean setterHasValidParameterSignature(Method setterMethod, Class<?> expectedType) {
        return setterMethod.getParameterCount() == 1 && expectedType.equals(setterMethod.getParameterTypes()[0]);
    }

    private boolean setterHasValidReturnType(Method setterMethod, Class<?> builderClass) {
        if (setterMethod.getReturnType() == void.class || setterMethod.getReturnType() == Void.class) {
            return true;
        }

        return setterMethod.getReturnType().isAssignableFrom(builderClass);
    }
}
