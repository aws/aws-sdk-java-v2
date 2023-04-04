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

package software.amazon.awssdk.codegen.poet.builder;

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.codegen.model.config.customization.AdditionalClientBuilder;
import software.amazon.awssdk.codegen.poet.ClientTestModels;
import software.amazon.awssdk.core.ClientType;

public class AdditionalClientBuilderInterfaceSpecTest {

    @ParameterizedTest
    @EnumSource(value = ClientType.class, names = {"SYNC", "ASYNC"})
    public void testSyncGeneration(ClientType type) {
        AdditionalClientBuilder testModel = getTestModel(type);
        assertThat(new AdditionalClientBuilderInterfaceSpec(ClientTestModels.queryServiceModels(), "CustomBuilder", testModel),
                   generatesTo(String.format("test-additional-%s-builder-class.java",
                                             type.toString().toLowerCase(Locale.ENGLISH))));
    }

    private static AdditionalClientBuilder getTestModel(ClientType type) {
        AdditionalClientBuilder model = new AdditionalClientBuilder();
        model.setClientType(type);

        Map<String, AdditionalClientBuilder.BuilderProperty> properties = new HashMap<>();

        AdditionalClientBuilder.BuilderProperty booleanProp = new AdditionalClientBuilder.BuilderProperty();
        booleanProp.setClassFqcn("java.lang.Boolean");
        booleanProp.setJavadoc("A boolean property");
        properties.put("booleanProperty", booleanProp);

        AdditionalClientBuilder.BuilderProperty stringProp = new AdditionalClientBuilder.BuilderProperty();
        stringProp.setClassFqcn("java.lang.String");
        stringProp.setJavadoc("A string property");
        properties.put("stringProperty", stringProp);

        AdditionalClientBuilder.BuilderProperty buildableProp = new AdditionalClientBuilder.BuilderProperty();
        buildableProp.setClassFqcn("software.amazon.awssdk.core.SomeBuildableClass");
        buildableProp.setJavadoc("A buildable property");
        buildableProp.setGenerateConsumerBuilder(true);
        properties.put("buildableProperty", buildableProp);

        model.setProperties(properties);

        return model;
    }
}
