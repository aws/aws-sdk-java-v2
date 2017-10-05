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

package software.amazon.awssdk.config;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

/**
 * Validate the functionality of the Client*Configuration classes
 */
public class ConfigurationBuilderTest {
    @Test
    public void overrideConfigurationClassHasExpectedMethods() throws Exception {
        assertConfigurationClassIsValid(ClientOverrideConfiguration.class);
    }

    private void assertConfigurationClassIsValid(Class<?> configurationClass) throws Exception {
        // Builders should be instantiable from the configuration class
        Method createBuilderMethod = configurationClass.getMethod("builder");
        Object builder = createBuilderMethod.invoke(null);

        // Builders should implement the configuration class's builder interface
        Class<?> builderInterface = Class.forName(configurationClass.getName() + "$Builder");
        assertThat(builder).isInstanceOf(builderInterface);

        Class<?> builderClass = builder.getClass();
        Method[] builderMethods = builderClass.getDeclaredMethods();

        // Builder's build methods should return the configuration object
        Optional<Method> buildMethod = Arrays.stream(builderMethods).filter(m -> m.getName().equals("build")).findFirst();
        assertThat(buildMethod).isPresent();
        Object builtObject = buildMethod.get().invoke(builder);
        assertThat(builtObject).isInstanceOf(configurationClass);

        // Analyze the builder for compliance with the bean specification
        BeanInfo builderBeanInfo = Introspector.getBeanInfo(builderClass);

        Map<String, PropertyDescriptor> builderProperties = Arrays.stream(builderBeanInfo.getPropertyDescriptors())
                .collect(toMap(PropertyDescriptor::getName, p -> p));

        // Validate method names
        for (Field field : configurationClass.getFields()) {
            // Ignore generated fields (eg. by Jacoco)
            if (field.isSynthetic()) {
                continue;
            }

            String builderPropertyName = builderClass.getSimpleName() + "'s " + field.getName() + " property";
            PropertyDescriptor builderProperty = builderProperties.get(field.getName());

            // Builders should have a bean-style write method for each field
            assertThat(builderProperty).as(builderPropertyName).isNotNull();
            assertThat(builderProperty.getReadMethod()).as(builderPropertyName + "'s read method").isNull();
            assertThat(builderProperty.getWriteMethod()).as(builderPropertyName + "'s write method").isNotNull();

            // Builders should have a fluent write method for each field
            Arrays.stream(builderMethods)
                  .filter(builderMethod -> matchesSignature(field.getName(), builderProperty, builderMethod))
                  .findAny()
                  .orElseThrow(() -> new AssertionError(builderClass + " can't write " + field.getName()));
        }
    }

    private boolean matchesSignature(String methodName, PropertyDescriptor property, Method builderMethod) {
        return builderMethod.getName().equals(methodName) &&
               builderMethod.getParameters().length == 1 &&
               builderMethod.getParameters()[0].getType().equals(property.getPropertyType());
    }
}
