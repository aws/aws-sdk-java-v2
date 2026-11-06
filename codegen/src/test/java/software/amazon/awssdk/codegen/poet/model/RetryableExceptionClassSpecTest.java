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

package software.amazon.awssdk.codegen.poet.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class RetryableExceptionClassSpecTest {
    private static IntermediateModel intermediateModel;

    @BeforeAll
    public static void setUp() throws IOException {
        File serviceModelFile =
            new File(RetryableExceptionClassSpecTest.class.getResource("exceptions/service-2.json").getFile());
        File customizationConfigFile = new File(RetryableExceptionClassSpecTest.class.getResource("exceptions/customization.config")
                                                                                     .getFile());
        intermediateModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile))
                     .customizationConfig(ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationConfigFile))
                     .build())
            .build();
    }

    @Test
    public void serviceCodegen_withErrorsWithRetryableTrait_ShouldOverrideIsRetryableAndIsThrottling() {
        intermediateModel.getShapes().forEach((name, shape) -> {
            if (shape.getShapeType() == ShapeType.Exception) {
                ClassSpec spec = new AwsServiceModel(intermediateModel, shape);
                assertThat(spec, generatesTo( "exceptions/" + name.toLowerCase() + ".java"));
            }
        });
    }

    @Test
    public void poetSpec_whenErrorCodeIsAuthenticationErrorCode_overridesIsAuthenticationError() {
        assertOnlyTheseOverrideIsAuthenticationError(intermediateModel,
                                                     Arrays.asList("ExpiredTokenException", "JsonServiceTokenExpiredException"));
    }

    @Test
    public void poetSpec_whenQueryCompatibleErrorCodeIsAuthenticationErrorCode_overridesIsAuthenticationError() {
        // QueryTokenExpired is registered by its shape name, but the service reports its ExpiredToken query error code.
        assertOnlyTheseOverrideIsAuthenticationError(queryCompatibleJsonModel(true),
                                                     Arrays.asList("ExpiredTokenException", "QueryTokenExpiredException"));
    }

    @Test
    public void poetSpec_whenJsonServiceIsNotQueryCompatible_ignoresErrorTraitCode() {
        // Without awsQueryCompatible, the client reports the shape name, so the ExpiredToken error code is never used.
        assertOnlyTheseOverrideIsAuthenticationError(queryCompatibleJsonModel(false),
                                                     Arrays.asList("ExpiredTokenException"));
    }

    private static IntermediateModel queryCompatibleJsonModel(boolean awsQueryCompatible) {
        File serviceModelFile =
            new File(RetryableExceptionClassSpecTest.class.getResource("querycompatibleexceptions/service-2.json").getFile());
        ServiceModel serviceModel = ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile);
        if (!awsQueryCompatible) {
            serviceModel.getMetadata().setAwsQueryCompatible(null);
        }
        return new IntermediateModelBuilder(C2jModels.builder()
                                                     .serviceModel(serviceModel)
                                                     .customizationConfig(CustomizationConfig.create())
                                                     .build())
            .build();
    }

    private static void assertOnlyTheseOverrideIsAuthenticationError(IntermediateModel model,
                                                                     List<String> authCodedExceptions) {
        List<String> exceptionNames = new ArrayList<>();
        model.getShapes().forEach((name, shape) -> {
            if (shape.getShapeType() == ShapeType.Exception) {
                exceptionNames.add(name);
                boolean overridesIsAuthenticationError =
                    new AwsServiceModel(model, shape).poetSpec()
                                                     .methodSpecs
                                                     .stream()
                                                     .anyMatch(m -> "isAuthenticationError".equals(m.name));
                Assertions.assertThat(overridesIsAuthenticationError)
                          .as("%s (error code %s, query compatible error code %s) overrides isAuthenticationError()",
                              name, shape.getErrorCode(), shape.getQueryCompatibleErrorCode())
                          .isEqualTo(authCodedExceptions.contains(name));
            }
        });
        Assertions.assertThat(exceptionNames).containsAll(authCodedExceptions);
    }
}
