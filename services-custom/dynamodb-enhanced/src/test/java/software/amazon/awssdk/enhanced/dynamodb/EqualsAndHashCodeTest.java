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
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import nl.jqno.equalsverifier.api.SingleTypeEqualsVerifierApi;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Test class for testing equals/hashCode methods for all enhanced DynamoDB classes in the main source set.
 */
public class EqualsAndHashCodeTest {

    private static final String ROOT_PACKAGE = "software.amazon.awssdk.enhanced.dynamodb";
    private static final String ROOT_PATH = ROOT_PACKAGE.replace('.', '/');
    private static final Pattern CLASS_PATTERN = Pattern.compile(".class", Pattern.LITERAL);


    @TestFactory
    Stream<DynamicTest> verifyEqualsAndHashCodeForAllMainClasses() throws Exception {
        List<Class<?>> testableClasses = findAllClassesUnderRootPackage()
            .stream()
            .filter(type -> isConcreteClass(type) && overridesEqualsOrHashCode(type))
            .collect(toList());

        return testableClasses.stream()
                              .map(this::createEqualsHashCodeTest)
                              .collect(toList())
                              .stream();
    }

    private DynamicTest createEqualsHashCodeTest(Class<?> type) {
        String testName = "equals/hashCode: " + type.getSimpleName();
        return DynamicTest.dynamicTest(testName, () -> verifyEqualsAndHashCode(type));
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
               && (file.getPath().contains("target/classes")
                   || file.getPath().contains("build/classes/java/main"));
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
            return (type.getDeclaredMethod("equals", Object.class).getDeclaringClass() != Object.class)
                   || (type.getDeclaredMethod("hashCode").getDeclaringClass() != Object.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private void verifyEqualsAndHashCode(Class<?> type) {
        SingleTypeEqualsVerifierApi<?> verifier =
            EqualsVerifier.forClass(type)
                          .withPrefabValues(
                              EnhancedType.class,
                              EnhancedType.of(String.class),
                              EnhancedType.of(Integer.class))
                          .withPrefabValues(
                              AttributeValue.class,
                              AttributeValue.builder().s("one").build(),
                              AttributeValue.builder().s("two").build());

        String className = type.getName();

        switch (className) {
            case "software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue": {
                verifier = verifier.withNonnullFields("type");
                break;
            }
            case "software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticIndexMetadata": {
                verifier = verifier.withNonnullFields("partitionKeys", "sortKeys")
                                   .usingGetClass();
                break;
            }
            case "software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticKeyAttributeMetadata": {
                verifier = verifier.withNonnullFields("order")
                                   .usingGetClass();
                break;
            }
            case "software.amazon.awssdk.enhanced.dynamodb.internal.document.DefaultEnhancedDocument": {
                // Provide non-equal prefab values for nonAttributeValueMap to avoid NullPointerException and Precondition error
                Map<String, String> map1 = new HashMap<>();
                Map<String, String> map2 = new HashMap<>();
                map2.put("key", "value");
                verifier = verifier.withPrefabValues(
                                       Map.class,
                                       map1,
                                       map2)
                                   .suppress(Warning.STRICT_HASHCODE)
                                   .withNonnullFields("nonAttributeValueMap",
                                                      "attributeValueMap",
                                                      "attributeConverterProviders",
                                                      "attributeConverterChain")
                                   .usingGetClass();
                break;
            }
            case "software.amazon.awssdk.enhanced.dynamodb.EnhancedType": {
                // Suppress warning about subclass equality
                verifier = verifier.suppress(nl.jqno.equalsverifier.Warning.STRICT_INHERITANCE)
                                   .withNonnullFields("rawClass")
                                   .usingGetClass();
                break;
            }
            case "software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest": {
                verifier = verifier.withIgnoredFields("ignoreNullsMode");
                break;
            }
            case "software.amazon.awssdk.enhanced.dynamodb.model.TransactUpdateItemEnhancedRequest": {
                verifier = verifier.withIgnoredFields("ignoreNullsMode").usingGetClass();
                break;
            }
            default: {
                if (Arrays.asList(
                              "software.amazon.awssdk.enhanced.dynamodb.internal.conditional.EqualToConditional",
                              "software.amazon.awssdk.enhanced.dynamodb.internal.conditional.SingleKeyItemConditional",
                              "software.amazon.awssdk.enhanced.dynamodb.internal.conditional.BetweenConditional",
                              "software.amazon.awssdk.enhanced.dynamodb.internal.conditional.BeginsWithConditional",
                              "software.amazon.awssdk.enhanced.dynamodb.internal.mapper.AtomicCounter$CounterAttribute",
                              "software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticKeyAttributeMetadata",
                              "software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext",
                              "software.amazon.awssdk.enhanced.dynamodb.internal.client.DefaultDynamoDbIndex",
                              "software.amazon.awssdk.enhanced.dynamodb.internal.client.DefaultDynamoDbTable",
                              "software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticIndexMetadata")
                          .contains(className)) {
                    verifier = verifier.usingGetClass();
                }
                break;
            }
        }

        verifier.verify();
    }
}