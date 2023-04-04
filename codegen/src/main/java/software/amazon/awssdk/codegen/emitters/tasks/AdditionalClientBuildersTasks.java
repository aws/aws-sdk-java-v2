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

package software.amazon.awssdk.codegen.emitters.tasks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.config.customization.AdditionalClientBuilder;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.builder.AdditionalClientBuilderInterfaceSpec;

public class AdditionalClientBuildersTasks extends BaseGeneratorTasks {

    private final IntermediateModel intermediateModel;

    public AdditionalClientBuildersTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        intermediateModel = dependencies.getModel();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        Map<String, AdditionalClientBuilder> additionalAsyncClientBuilders =
            intermediateModel.getCustomizationConfig().getAdditionalClientBuilders();

        if (additionalAsyncClientBuilders == null) {
            return Collections.emptyList();
        }

        return additionalAsyncClientBuilders.entrySet()
                                            .stream()
                                            .map(e -> {
                                                String name = e.getKey();
                                                AdditionalClientBuilder model = e.getValue();
                                                ClassSpec spec = new AdditionalClientBuilderInterfaceSpec(intermediateModel,
                                                                                                          name, model);
                                                return createPoetGeneratorTask(spec);
                                            })
                                            .collect(Collectors.toList());
    }
}
