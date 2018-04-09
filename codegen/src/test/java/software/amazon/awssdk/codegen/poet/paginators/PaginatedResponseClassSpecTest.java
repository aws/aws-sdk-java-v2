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

package software.amazon.awssdk.codegen.poet.paginators;

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.io.File;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class PaginatedResponseClassSpecTest {

    private static IntermediateModel intermediateModel;
    private static Paginators paginators;

    @BeforeClass
    public static void setUp() throws IOException {
        File serviceModelFile = new File(PaginatedResponseClassSpecTest.class.getResource("service-2.json").getFile());
        File customizationConfigFile = new File(PaginatedResponseClassSpecTest.class.getResource("customization.config")
                                                                                    .getFile());
        File paginatorsModel = new File(PaginatedResponseClassSpecTest.class.getResource("paginators.json")
                                                                            .getFile());

        paginators = getPaginatorsModel(paginatorsModel);

        intermediateModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile))
                     .customizationConfig(ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationConfigFile))
                     .paginatorsModel(paginators)
                     .build())
            .build();
    }

    private static Paginators getPaginatorsModel(File file) {
        return ModelLoaderUtils.loadModel(Paginators.class, file);
    }

    @Test
    public void testGeneratedResponseForSyncOperations() {
        paginators.getPaginators().entrySet()
                  .stream()
                  .filter(entry -> entry.getValue().isValid())
                  .forEach(entry ->
                           {
                               ClassSpec classSpec = new SyncResponseClassSpec(intermediateModel,
                                                                               entry.getKey(),
                                                                               entry.getValue());
                               assertThat(classSpec, generatesTo(classSpec.className().simpleName() + ".java"));
                           });
    }

    @Test
    public void testGeneratedResponseForAsyncOperations() {
        paginators.getPaginators().entrySet()
                  .stream()
                  .filter(entry -> entry.getValue().isValid())
                  .forEach(entry ->
                           {
                               ClassSpec classSpec = new AsyncResponseClassSpec(intermediateModel,
                                                                               entry.getKey(),
                                                                               entry.getValue());
                               assertThat(classSpec, generatesTo(classSpec.className().simpleName() + ".java"));
                           });
    }
}
