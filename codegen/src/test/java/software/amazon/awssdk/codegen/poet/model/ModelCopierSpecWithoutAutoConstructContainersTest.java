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

package software.amazon.awssdk.codegen.poet.model;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

/**
 * Similar to {@link ModelCopierSpecTest} but tests correct generation when auto construct containers are disabled.
 */
@RunWith(Parameterized.class)
public class ModelCopierSpecWithoutAutoConstructContainersTest {
    private static File serviceModelFile;
    private static IntermediateModel intermediateModel;
    private final ClassSpec spec;
    private final String specName;

    private static void setUp() throws URISyntaxException, IOException {
        serviceModelFile = new File(AwsModelSpecTest.class
                .getResource("service-2.json")
                .getFile());

        File customizationConfigFile = new File(AwsModelSpecTest.class
                .getResource("customization.config")
                .getFile());

        CustomizationConfig customizationConfig = ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationConfigFile);
        customizationConfig.setUseAutoConstructList(false);
        customizationConfig.setUseAutoConstructMap(false);

        intermediateModel = new IntermediateModelBuilder(
                C2jModels.builder()
                        .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile))
                        .customizationConfig(customizationConfig)
                        .build())
                .build();
    }

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data() {
        invokeSafely(ModelCopierSpecWithoutAutoConstructContainersTest::setUp);
        return new ServiceModelCopiers(intermediateModel).copierSpecs().stream()
                                                         .map(spec -> new Object[] { spec, spec.className().simpleName().toLowerCase(Locale.ENGLISH) })
                                                         .collect(toList());
    }

    public ModelCopierSpecWithoutAutoConstructContainersTest(ClassSpec spec, String specName) {
        this.spec = spec;
        this.specName = specName;
    }

    @Test
    public void basicGeneration() {
        assertThat(spec, generatesTo(expectedFile()));
    }

    private String expectedFile() {
        String name = specName + ".java";
        String autoConstructVariant = "./nonautoconstructcontainers/" + name;
        if (getClass().getResource(autoConstructVariant) != null) {
            return autoConstructVariant;
        }
        return name;
    }
}
