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

package software.amazon.awssdk.codegen.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.CodeGenerator;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.rules.endpoints.EndpointTestSuiteModel;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Waiters;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;
import software.amazon.awssdk.utils.StringUtils;

/**
 * The Maven mojo to generate Java client code using software.amazon.awssdk:codegen module.
 */
@Mojo(name = "generate")
public class GenerationMojo extends AbstractMojo {
    private static final String MODEL_FILE = "service-2.json";
    private static final String CUSTOMIZATION_CONFIG_FILE = "customization.config";
    private static final String WAITERS_FILE = "waiters-2.json";
    private static final String PAGINATORS_FILE = "paginators-1.json";
    private static final String ENDPOINT_RULE_SET_FILE = "endpoint-rule-set.json";
    private static final String ENDPOINT_TESTS_FILE = "endpoint-tests.json";


    @Parameter(property = "codeGenResources", defaultValue = "${basedir}/src/main/resources/codegen-resources/")
    private File codeGenResources;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}")
    private String outputDirectory;

    @Parameter(property = "writeIntermediateModel", defaultValue = "false")
    private boolean writeIntermediateModel;

    @Parameter(property = "writeValidationReport", defaultValue = "false")
    private boolean writeValidationReport;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    private Path sourcesDirectory;
    private Path resourcesDirectory;
    private Path testsDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        this.sourcesDirectory = Paths.get(outputDirectory).resolve("generated-sources").resolve("sdk");
        this.resourcesDirectory = Paths.get(outputDirectory).resolve("generated-resources").resolve("sdk-resources");
        this.testsDirectory = Paths.get(outputDirectory).resolve("generated-test-sources").resolve("sdk-tests");

        List<GenerationParams> generationParams = initGenerationParams();

        Map<String, IntermediateModel> serviceNameToModelMap =
            generationParams.stream().collect(Collectors.toMap(
                params -> {
                    IntermediateModel model = params.intermediateModel;
                    return StringUtils.lowerCase(model.getMetadata().getServiceName());
                },
                params -> params.intermediateModel));

        // Update each param with the intermediate model it shares models with, if any
        generationParams.forEach(params -> {
            CustomizationConfig customizationConfig = params.intermediateModel.getCustomizationConfig();

            if (customizationConfig.getShareModelConfig() != null) {
                String shareModelWithName = customizationConfig.getShareModelConfig().getShareModelWith();
                params.withShareModelsTarget(serviceNameToModelMap.get(shareModelWithName));
            }
        });

        generationParams.forEach(this::generateCode);

        project.addCompileSourceRoot(sourcesDirectory.toFile().getAbsolutePath());
        project.addTestCompileSourceRoot(testsDirectory.toFile().getAbsolutePath());
    }

    private List<GenerationParams> initGenerationParams() throws MojoExecutionException {
        List<ModelRoot> modelRoots = findModelRoots().collect(Collectors.toList());

        return modelRoots.stream().map(r -> {
            Path modelRootPath = r.modelRoot;
            getLog().info("Loading from: " + modelRootPath.toString());
            C2jModels c2jModels = C2jModels.builder()
                                           .customizationConfig(r.customizationConfig)
                                           .serviceModel(loadServiceModel(modelRootPath))
                                           .waitersModel(loadWaiterModel(modelRootPath))
                                           .paginatorsModel(loadPaginatorModel(modelRootPath))
                                           .endpointRuleSetModel(loadEndpointRuleSetModel(modelRootPath))
                                           .endpointTestSuiteModel(loadEndpointTestSuiteModel(modelRootPath))
                                           .build();
            String intermediateModelFileNamePrefix = intermediateModelFileNamePrefix(c2jModels);
            IntermediateModel intermediateModel = new IntermediateModelBuilder(c2jModels).build();
            return new GenerationParams().withIntermediateModel(intermediateModel)
                                         .withIntermediateModelFileNamePrefix(intermediateModelFileNamePrefix);
        }).collect(Collectors.toList());
    }

    private Stream<ModelRoot> findModelRoots() throws MojoExecutionException {
        try {
            return Files.find(codeGenResources.toPath(), 10, this::isModelFile)
                        .map(Path::getParent)
                        .map(p -> new ModelRoot(p, loadCustomizationConfig(p)))
                        .sorted(this::modelSharersLast);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to find '" + MODEL_FILE + "' files in " + codeGenResources, e);
        }
    }

    private int modelSharersLast(ModelRoot lhs, ModelRoot rhs) {
        return lhs.customizationConfig.getShareModelConfig() == null ? -1 : 1;
    }

    private boolean isModelFile(Path p, BasicFileAttributes a) {
        return p.toString().endsWith(MODEL_FILE);
    }

    private void generateCode(GenerationParams params) {
        CodeGenerator.builder()
                     .intermediateModel(params.intermediateModel)
                     .shareModelsTarget(params.shareModelsTarget)
                     .sourcesDirectory(sourcesDirectory.toFile().getAbsolutePath())
                     .resourcesDirectory(resourcesDirectory.toFile().getAbsolutePath())
                     .testsDirectory(testsDirectory.toFile().getAbsolutePath())
                     .intermediateModelFileNamePrefix(params.intermediateModelFileNamePrefix)
                     .build()
                     .execute();
    }

    private String intermediateModelFileNamePrefix(C2jModels models) {
        return writeIntermediateModel ? Utils.getFileNamePrefix(models.serviceModel()) : null;
    }

    private CustomizationConfig loadCustomizationConfig(Path root) {
        return ModelLoaderUtils.loadOptionalModel(CustomizationConfig.class,
                                                  root.resolve(CUSTOMIZATION_CONFIG_FILE).toFile(),
                                                  true)
                               .orElse(CustomizationConfig.create());
    }

    private ServiceModel loadServiceModel(Path root) {
        return loadRequiredModel(ServiceModel.class, root.resolve(MODEL_FILE));
    }

    private Waiters loadWaiterModel(Path root) {
        return loadOptionalModel(Waiters.class, root.resolve(WAITERS_FILE)).orElse(Waiters.none());
    }

    private Paginators loadPaginatorModel(Path root) {
        return loadOptionalModel(Paginators.class, root.resolve(PAGINATORS_FILE)).orElse(Paginators.none());
    }

    private EndpointRuleSetModel loadEndpointRuleSetModel(Path root) {
        return loadOptionalModel(EndpointRuleSetModel.class, root.resolve(ENDPOINT_RULE_SET_FILE)).orElse(null);
    }

    private EndpointTestSuiteModel loadEndpointTestSuiteModel(Path root) {
        return loadOptionalModel(EndpointTestSuiteModel.class, root.resolve(ENDPOINT_TESTS_FILE)).orElse(null);
    }

    /**
     * Load required model from the project resources.
     */
    private <T> T loadRequiredModel(Class<T> clzz, Path location) {
        return ModelLoaderUtils.loadModel(clzz, location.toFile());
    }

    /**
     * Load an optional model from the project resources.
     *
     * @return Model or empty optional if not present.
     */
    private <T> Optional<T> loadOptionalModel(Class<T> clzz, Path location) {
        return ModelLoaderUtils.loadOptionalModel(clzz, location.toFile());
    }

    private static class ModelRoot {
        private final Path modelRoot;
        private final CustomizationConfig customizationConfig;

        private ModelRoot(Path modelRoot, CustomizationConfig customizationConfig) {
            this.modelRoot = modelRoot;
            this.customizationConfig = customizationConfig;
        }
    }

    private static class GenerationParams {
        private IntermediateModel intermediateModel;
        private IntermediateModel shareModelsTarget;
        private String intermediateModelFileNamePrefix;

        public GenerationParams withIntermediateModel(IntermediateModel intermediateModel) {
            this.intermediateModel = intermediateModel;
            return this;
        }

        public GenerationParams withShareModelsTarget(IntermediateModel shareModelsTarget) {
            this.shareModelsTarget = shareModelsTarget;
            return this;
        }

        public GenerationParams withIntermediateModelFileNamePrefix(String intermediateModelFileNamePrefix) {
            this.intermediateModelFileNamePrefix = intermediateModelFileNamePrefix;
            return this;
        }
    }
}
