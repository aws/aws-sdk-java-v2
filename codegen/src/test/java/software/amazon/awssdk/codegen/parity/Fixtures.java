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
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

/**
 * Loads a service's codegen resources from
 * {@code services/<name>/src/main/resources/codegen-resources/} and builds an
 * {@link IntermediateModel}. Reads files in place — no copying.
 */
final class Fixtures {

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

    static IntermediateModel buildFromSmithy(String serviceName) {
        // TODO: swap to SmithyIntermediateModelBuilder once Smithy translation lands.
        return buildFromC2j(serviceName);
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
