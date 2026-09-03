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

package software.amazon.awssdk.codegen.parity;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.EndpointTestSuiteModel;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Waiters;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyTransformChain;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;
import software.amazon.smithy.model.Model;

/**
 * Loads a service's codegen resources from
 * {@code services/<name>/src/main/resources/codegen-resources/} and builds an
 * {@link IntermediateModel}. Reads files in place — no copying.
 */
final class Fixtures {

    /**
     * Smithy models for POC services live in test resources rather than in the shipped service
     * directory, so that every real service build stays on the C2J path.
     */
    private static final String POC_FIXTURE_ROOT = "/software/amazon/awssdk/codegen/poc/transforms";

    private Fixtures() {
    }

    static IntermediateModel buildFromC2j(String serviceName) {
        Path dir = servicePath(serviceName);
        ServiceModel service = ModelLoaderUtils.loadModel(ServiceModel.class,
                                                          dir.resolve("service-2.json").toFile());

        CustomizationConfig customization = ModelLoaderUtils
            .loadOptionalModel(CustomizationConfig.class, dir.resolve("customization.config").toFile())
            .orElseGet(CustomizationConfig::create);

        Paginators paginators = ModelLoaderUtils
            .loadOptionalModel(Paginators.class, dir.resolve("paginators-1.json").toFile())
            .orElseGet(Paginators::none);

        Waiters waiters = ModelLoaderUtils
            .loadOptionalModel(Waiters.class, dir.resolve("waiters-2.json").toFile())
            .orElseGet(Waiters::none);

        EndpointRuleSetModel endpointRuleSet = ModelLoaderUtils
            .loadOptionalModel(EndpointRuleSetModel.class, dir.resolve("endpoint-rule-set.json").toFile())
            .orElse(null);

        EndpointTestSuiteModel endpointTests = ModelLoaderUtils
            .loadOptionalModel(EndpointTestSuiteModel.class, dir.resolve("endpoint-tests.json").toFile())
            .orElse(null);

        C2jModels models = C2jModels.builder()
                                    .serviceModel(service)
                                    .customizationConfig(customization)
                                    .paginatorsModel(paginators)
                                    .waitersModel(waiters)
                                    .endpointRuleSetModel(endpointRuleSet)
                                    .endpointTestSuiteModel(endpointTests)
                                    .build();

        return new IntermediateModelBuilder(models).build();
    }

    /**
     * Legacy entry point for services that have no Smithy model in the tree. Still a pass-through, so
     * {@link IntermediateModelParityTest} keeps comparing a model to itself for those services.
     *
     * <p>Services that do have a Smithy fixture go through
     * {@link #buildFromSmithy(String, boolean)} instead.
     */
    static IntermediateModel buildFromSmithy(String serviceName) {
        return buildFromC2j(serviceName);
    }

    /**
     * Builds an {@link IntermediateModel} from a POC service's Smithy model, optionally applying the
     * declarative transforms declared beside it.
     *
     * <p>The two modes are the second and third legs of the three-way comparison: transforms off and
     * transforms on with everything else held constant, so the difference between the two results is
     * exactly what the transforms accomplished, with none of the noise the unconditional C2J
     * processors introduce.
     *
     * <p>The customization config is passed through untouched in both modes. It does not need
     * stripping, because the five model-editing settings are read only by the C2J processor chain and
     * are invisible to {@link SmithyIntermediateModelBuilder}.
     */
    static IntermediateModel buildFromSmithy(String serviceName, boolean applyTransforms) {
        Path fixtureDir = pocFixturePath(serviceName);
        ClassLoader classLoader = Fixtures.class.getClassLoader();

        // Tolerant assembly, not unwrap(): real AWS models carry peripheral traits whose validators
        // can raise errors even when the structure codegen reads is perfectly fine.
        Model assembled = Model.assembler(classLoader)
                               .discoverModels(classLoader)
                               .addImport(fixtureDir.resolve("model.json"))
                               .assemble()
                               .getResult()
                               .orElseThrow(() -> new IllegalStateException(
                                   "Could not assemble the Smithy model for " + serviceName));

        Model model = applyTransforms
                      ? SmithyTransformChain.applyIfPresent(assembled, fixtureDir, classLoader)
                      : assembled;

        SmithyModels models = SmithyModels.builder()
                                          .model(model)
                                          .customizationConfig(loadCustomization(serviceName))
                                          .build();

        return new SmithyIntermediateModelBuilder(models).build();
    }

    /**
     * Resolves a POC fixture directory to a real path. Test resources are copied to the output
     * directory, so both the model and its transform config are reachable as files, which lets the
     * harness use the same file-presence trigger the mojo uses.
     */
    static Path pocFixturePath(String serviceName) {
        String resource = POC_FIXTURE_ROOT + "/" + serviceName;
        URL url = Fixtures.class.getResource(resource);
        if (url == null) {
            throw new IllegalArgumentException("No POC Smithy fixture on the classpath at " + resource);
        }
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Could not resolve POC fixture directory " + resource, e);
        }
    }

    static CustomizationConfig loadCustomization(String serviceName) {
        return ModelLoaderUtils
            .loadOptionalModel(CustomizationConfig.class,
                               servicePath(serviceName).resolve("customization.config").toFile())
            .orElseGet(CustomizationConfig::create);
    }

    private static Path servicePath(String serviceName) {
        Path dir = Paths.get("..", "services", serviceName, "src", "main", "resources", "codegen-resources");
        File asFile = dir.toFile();
        if (!asFile.exists() || !asFile.isDirectory()) {
            throw new IllegalArgumentException("Codegen resources directory not found: " + asFile.getAbsolutePath());
        }
        return dir;
    }
}
