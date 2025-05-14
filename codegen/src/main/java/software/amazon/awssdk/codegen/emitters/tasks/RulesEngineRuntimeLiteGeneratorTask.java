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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;

/** A version of {@link software.amazon.awssdk.codegen.emitters.tasks.RulesEngineRuntimeGeneratorTask} that copies a minimal
 * set of the interpreter related classes. This set represents the only classes that need to be copied when compiled rules are
 * enabled.
 *
 * @see CustomizationConfig#isEnableGenerateCompiledEndpointRules()
 */
public final class RulesEngineRuntimeLiteGeneratorTask extends RulesEngineRuntimeGeneratorTask {
    private static final List<String> FILES_TO_COPY = Stream.of("/Outputs.java.resource",
                                                                "/RegionOverride.java.resource",
                                                                "/Partition.java.resource",
                                                                "/PartitionDataProvider.java.resource",
                                                                "/AwsEndpointProviderUtils.java.resource",
                                                                "/Arn.java.resource",
                                                                "/Value.java.resource",
                                                                "/Identifier.java.resource",
                                                                "/EndpointAuthSchemeStrategy.java.resource",
                                                                "/EndpointAttributeProvider.java.resource",
                                                                "/EndpointAuthSchemeStrategyFactory.java.resource",
                                                                "/DefaultEndpointAuthSchemeStrategy.java.resource")
                                                            .collect(Collectors.toList());

    public RulesEngineRuntimeLiteGeneratorTask(GeneratorTaskParams generatorTaskParams) {
        super(generatorTaskParams);
    }

    protected List<String> rulesEngineJavaFilePaths(Collection<String> runtimeEngineFiles) {
        return super.rulesEngineJavaFilePaths(runtimeEngineFiles)
                    .stream()
                    .filter(e -> FILES_TO_COPY.stream().anyMatch(e::endsWith))
                    .collect(Collectors.toList());
    }
}
