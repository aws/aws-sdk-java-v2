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

package software.amazon.awssdk.enhanced.dynamodb;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Test class that discovers all interfaces with default methods that throw UnsupportedOperationException. Shows individual test
 * scenarios and results using DynamicTest.
 */
public class DefaultMethodsUnsupportedOperationTest {

    private static final String BASE_PACKAGE = "software.amazon.awssdk.enhanced.dynamodb";
    private static final Pattern CLASS_PATTERN = Pattern.compile(".class", Pattern.LITERAL);

    private static final List<String> testScenarios = Collections.synchronizedList(new java.util.ArrayList<>());

    @TestFactory
    Stream<DynamicTest> testDefaultMethodsThrowUnsupportedOperation() {
        List<DynamicTest> dynamicTestList = scanPackageForClasses(BASE_PACKAGE)
            .filter(Class::isInterface)
            .filter(this::hasDefaultMethods)
            .collect(toList())
            .stream()
            .flatMap(this::createTestsForInterface)
            .collect(toList());
        assertEquals(100, dynamicTestList.size());
        return dynamicTestList.stream();
    }

    private Stream<Class<?>> scanPackageForClasses(String packageName) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            return Collections.list(loader.getResources(packageName.replace('.', '/')))
                              .stream()
                              .map(URL::getFile)
                              .map(File::new)
                              .filter(File::exists)
                              .flatMap(dir -> findClassesInDirectory(dir, packageName));
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    private Stream<Class<?>> findClassesInDirectory(File dir, String packageName) {
        return Optional.ofNullable(dir.listFiles())
                       .map(Arrays::stream)
                       .orElseGet(Stream::empty)
                       .flatMap(file ->
                                    file.isDirectory()
                                    ? findClassesInDirectory(file, packageName + "." + file.getName())
                                    : loadClassFromFile(file, packageName));
    }

    private Stream<Class<?>> loadClassFromFile(File file, String packageName) {
        if (!file.getName().endsWith(".class")) {
            return Stream.empty();
        }

        String className = packageName + '.' + CLASS_PATTERN.matcher(file.getName()).replaceAll("");
        try {
            return Stream.of(Class.forName(className));
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return Stream.empty();
        }
    }

    private boolean hasDefaultMethods(Class<?> interfaceClass) {
        return Arrays.stream(interfaceClass.getDeclaredMethods())
                     .anyMatch(Method::isDefault);
    }

    private Stream<DynamicTest> createTestsForInterface(Class<?> interfaceClass) {
        return Arrays.stream(interfaceClass.getDeclaredMethods())
                     .filter(Method::isDefault)
                     .filter(method -> throwsUnsupportedOperation(interfaceClass, method))
                     .map(method -> {
                         String testName = String.format("%s.%s() → throws UnsupportedOperationException",
                                                         interfaceClass.getSimpleName(),
                                                         method.getName());
                         testScenarios.add(testName);

                         return DynamicTest.dynamicTest(testName, () ->
                             testMethodThrowsUnsupportedOperation(interfaceClass, method));
                     });
    }

    private boolean throwsUnsupportedOperation(Class<?> interfaceClass, Method method) {
        try {
            Object mockInstance = createMockInstance(interfaceClass);
            Object[] args = createArguments(method);
            method.invoke(mockInstance, args);
            return false;
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return cause instanceof UnsupportedOperationException;
        }
    }

    private void testMethodThrowsUnsupportedOperation(Class<?> interfaceClass, Method method) {
        Object mockInstance = createMockInstance(interfaceClass);
        Object[] args = createArguments(method);

        assertThrows(UnsupportedOperationException.class, () -> {
            try {
                method.invoke(mockInstance, args);
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                if (cause instanceof UnsupportedOperationException) {
                    throw cause;
                }
                throw new RuntimeException(cause);
            }
        }, () -> String.format("Expected %s.%s() to throw UnsupportedOperationException",
                               interfaceClass.getSimpleName(), method.getName()));
    }

    private <T> T createMockInstance(Class<T> interfaceClass) {
        T mock = mock(interfaceClass, CALLS_REAL_METHODS);
        if (mock instanceof MappedTableResource) {
            when(((MappedTableResource<?>) mock).tableName()).thenReturn("test-table");
        }
        return mock;
    }

    private Object[] createArguments(Method method) {
        return Arrays.stream(method.getParameterTypes()).map(this::createArgument).toArray();
    }

    private Object createArgument(Class<?> paramType) {
        if (paramType == String.class) {
            return "test";
        }
        if (paramType == Key.class) {
            return Key.builder().partitionValue("test").build();
        }
        if (Consumer.class.isAssignableFrom(paramType)) {
            return (Consumer<?>) obj -> {
            };
        }
        if (paramType.isInterface()) {
            return mock(paramType);
        }
        try {
            return mock(paramType);
        } catch (Exception e) {
            return null;
        }
    }
}