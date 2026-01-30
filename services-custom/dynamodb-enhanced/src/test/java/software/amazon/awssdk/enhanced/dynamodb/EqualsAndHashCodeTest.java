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

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isInterface;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Test class for testing equals/hashCode methods for all enhanced DynamoDB classes in the main source set.
 */
public class EqualsAndHashCodeTest {

    private static final String ROOT_PACKAGE = "software.amazon.awssdk.enhanced.dynamodb";
    private static final String ROOT_PATH = ROOT_PACKAGE.replace('.', '/');
    private static final Pattern CLASS_PATTERN = Pattern.compile(".class", Pattern.LITERAL);

    @Test
    public void verifyEqualsAndHashCodeForAllMainClasses() throws Exception {
        findAllClassesUnderRootPackage()
            .stream()
            .filter(type ->
                        isConcreteClass(type)
                        && overridesEqualsOrHashCode(type))
            .forEach(this::verifyEqualsAndHashCode);
    }

    private List<Class<?>> findAllClassesUnderRootPackage() throws Exception {
        List<Class<?>> classes = new ArrayList<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(ROOT_PATH);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (!"file".equals(resource.getProtocol())) {
                continue;
            }

            URI uri = resource.toURI();
            File directory = new File(uri);
            scanDirectory(directory, ROOT_PACKAGE, classes);
        }

        return classes;
    }

    private void scanDirectory(File dir, String pkg, List<Class<?>> classes) throws ClassNotFoundException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, pkg + "." + file.getName(), classes);
            } else if (isMainClass(file)) {
                classes.add(Class.forName(pkg + '.' + CLASS_PATTERN.matcher(file.getName()).replaceAll("")));
            }
        }
    }

    private boolean isMainClass(File file) {
        return file.getName().endsWith(".class")
               && file.getPath().contains("target/classes")
               || file.getPath().contains("build/classes/java/main");
    }

    private boolean isConcreteClass(Class<?> type) {
        int m = type.getModifiers();
        return !isAbstract(m)
               && !isInterface(m)
               && !type.isEnum()
               && !type.isAnonymousClass()
               && !type.isLocalClass();
    }

    private boolean overridesEqualsOrHashCode(Class<?> type) {
        try {
            return
                (type.getDeclaredMethod("equals", Object.class).getDeclaringClass() != Object.class)
                || (type.getDeclaredMethod("hashCode").getDeclaringClass() != Object.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private void verifyEqualsAndHashCode(Class<?> type) {
        EqualsVerifier.forClass(type)
                      .withPrefabValues(
                          EnhancedType.class,
                          EnhancedType.of(String.class),
                          EnhancedType.of(Integer.class))
                      .withPrefabValues(
                          AttributeValue.class,
                          AttributeValue.builder().s("one").build(),
                          AttributeValue.builder().s("two").build())
                      .suppress(
                          Warning.NULL_FIELDS,
                          Warning.STRICT_INHERITANCE,
                          Warning.ALL_FIELDS_SHOULD_BE_USED,
                          Warning.STRICT_HASHCODE)
                      .verify();
    }
}