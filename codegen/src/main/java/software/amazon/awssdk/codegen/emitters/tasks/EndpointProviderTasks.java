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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.service.ClientContextParam;
import software.amazon.awssdk.codegen.poet.rules.ClientContextParamsClassSpec;
import software.amazon.awssdk.codegen.poet.rules.DefaultPartitionDataProviderSpec;
import software.amazon.awssdk.codegen.poet.rules.EndpointParametersClassSpec;
import software.amazon.awssdk.codegen.poet.rules.EndpointProviderInterfaceSpec;
import software.amazon.awssdk.codegen.poet.rules.EndpointProviderSpec;
import software.amazon.awssdk.codegen.poet.rules.EndpointProviderTestSpec;
import software.amazon.awssdk.codegen.poet.rules.EndpointResolverInterceptorSpec;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesClientTestSpec;
import software.amazon.awssdk.codegen.poet.rules.RequestEndpointInterceptorSpec;

public final class EndpointProviderTasks extends BaseGeneratorTasks {
    private final GeneratorTaskParams generatorTaskParams;

    public EndpointProviderTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.generatorTaskParams = dependencies;
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        List<GeneratorTask> tasks = new ArrayList<>();
        tasks.add(generateInterface());
        tasks.add(generateParams());
        tasks.add(generateDefaultProvider());
        tasks.add(new RulesEngineRuntimeGeneratorTask(generatorTaskParams));
        if (shouldGenerateJmesPathRuntime()) {
            tasks.add(new JmesPathRuntimeGeneratorTask(generatorTaskParams));
        }
        tasks.addAll(generateInterceptors());
        if (shouldGenerateEndpointTests()) {
            tasks.add(generateProviderTests());
        }
        if (shouldGenerateEndpointTests() && shouldGenerateClientEndpointTests()) {
            tasks.add(generateClientTests());
        }
        if (hasClientContextParams()) {
            tasks.add(generateClientContextParams());
        }
        tasks.add(generateDefaultPartitionsProvider());
        return tasks;
    }

    private GeneratorTask generateInterface() {
        return new PoetGeneratorTask(endpointRulesDir(), model.getFileHeader(), new EndpointProviderInterfaceSpec(model));
    }

    private GeneratorTask generateParams() {
        return new PoetGeneratorTask(endpointRulesDir(), model.getFileHeader(), new EndpointParametersClassSpec(model));
    }

    private GeneratorTask generateDefaultProvider() {
        return new PoetGeneratorTask(endpointRulesInternalDir(), model.getFileHeader(), new EndpointProviderSpec(model));
    }

    private GeneratorTask generateDefaultPartitionsProvider() {
        return new PoetGeneratorTask(endpointRulesInternalDir(), model.getFileHeader(),
                                     new DefaultPartitionDataProviderSpec(model));
    }

    private Collection<GeneratorTask> generateInterceptors() {
        return Arrays.asList(
            new PoetGeneratorTask(endpointRulesInternalDir(), model.getFileHeader(), new EndpointResolverInterceptorSpec(model)),
            new PoetGeneratorTask(endpointRulesInternalDir(), model.getFileHeader(), new RequestEndpointInterceptorSpec(model)));
    }

    private GeneratorTask generateClientTests() {
        return new PoetGeneratorTask(endpointTestsDir(), model.getFileHeader(), new EndpointRulesClientTestSpec(model));
    }

    private GeneratorTask generateProviderTests() {
        return new PoetGeneratorTask(endpointTestsDir(), model.getFileHeader(), new EndpointProviderTestSpec(model));
    }

    private GeneratorTask generateClientContextParams() {
        return new PoetGeneratorTask(endpointRulesInternalDir(), model.getFileHeader(), new ClientContextParamsClassSpec(model));
    }

    private String endpointRulesDir() {
        return generatorTaskParams.getPathProvider().getEndpointRulesDirectory();
    }

    private String endpointRulesInternalDir() {
        return generatorTaskParams.getPathProvider().getEndpointRulesInternalDirectory();
    }

    private String endpointTestsDir() {
        return generatorTaskParams.getPathProvider().getEndpointRulesTestDirectory();
    }

    private boolean shouldGenerateEndpointTests() {
        CustomizationConfig customizationConfig = generatorTaskParams.getModel().getCustomizationConfig();
        return !Boolean.TRUE.equals(customizationConfig.isSkipEndpointTestGeneration()) &&
               !generatorTaskParams.getModel().getEndpointTestSuiteModel().getTestCases().isEmpty();
    }

    private boolean shouldGenerateClientEndpointTests() {
        boolean generateEndpointClientTests = generatorTaskParams.getModel()
                                                                 .getCustomizationConfig()
                                                                 .isGenerateEndpointClientTests();
        boolean someTestCasesHaveOperationInputs = model.getEndpointTestSuiteModel().getTestCases().stream()
                                                        .anyMatch(t -> t.getOperationInputs() != null);
        return generateEndpointClientTests || someTestCasesHaveOperationInputs;
    }

    private boolean hasClientContextParams() {
        Map<String, ClientContextParam> clientContextParams = model.getClientContextParams();
        Map<String, ClientContextParam> customClientContextParams = model.getCustomizationConfig().getCustomClientContextParams();
        return (clientContextParams != null && !clientContextParams.isEmpty()) ||
               (customClientContextParams != null && !customClientContextParams.isEmpty());
    }

    private boolean shouldGenerateJmesPathRuntime() {
        boolean isAlreadyGenerated = model.hasWaiters();
        if (isAlreadyGenerated) {
            return true;
        }

        Map<String, ParameterModel> endpointParameters = model.getCustomizationConfig().getEndpointParameters();
        if (endpointParameters == null) {
            return false;
        }

        return endpointParameters.values().stream().anyMatch(this::paramRequiresPathParserRuntime);
    }

    private boolean paramRequiresPathParserRuntime(ParameterModel parameterModel) {
        return paramIsOperationalContextParam(parameterModel) &&
               "stringarray".equals(parameterModel.getType().toLowerCase(Locale.US));
    }

    //TODO (string-array-params): resolve this logical test before finalizing coding
    private boolean paramIsOperationalContextParam(ParameterModel parameterModel) {
        return true;
    }
}
