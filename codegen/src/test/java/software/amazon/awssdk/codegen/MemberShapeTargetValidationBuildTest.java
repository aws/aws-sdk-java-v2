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

package software.amazon.awssdk.codegen;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;
import software.amazon.awssdk.codegen.validation.ModelInvalidException;
import software.amazon.awssdk.codegen.validation.ValidationEntry;
import software.amazon.awssdk.codegen.validation.ValidationErrorId;
import software.amazon.awssdk.codegen.validation.ValidationErrorSeverity;

/**
 * Full-build coverage proving that member-to-shape linking actually leaves a null shape when a referenced target is removed by
 * a customization, and that {@link IntermediateModelBuilder#build()} fails fast instead of producing a downstream
 * {@link NullPointerException}. Reproduces P456014803.
 */
public class MemberShapeTargetValidationBuildTest {

    @Test
    public void build_enumTargetRemovedByDeprecatedShapesCustomization_throwsModelInvalid() {
        File serviceModelFile = new File(MemberShapeTargetValidationBuildTest.class
            .getResource("poet/client/c2j/dangling-shape-validator/service-2.json").getFile());
        File customizationFile = new File(MemberShapeTargetValidationBuildTest.class
            .getResource("poet/client/c2j/dangling-shape-validator/customization.config").getFile());

        C2jModels models = C2jModels.builder()
            .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile))
            .customizationConfig(ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationFile))
            .build();

        assertThatThrownBy(() -> new IntermediateModelBuilder(models).build())
            .isInstanceOf(ModelInvalidException.class)
            .hasMessageContaining("GetRequestAuthorizationDetailsResponse")
            .hasMessageContaining("AuthorizationDetails")
            .hasMessageContaining("AuthDetailType")
            .matches(e -> {
                ValidationEntry entry = ((ModelInvalidException) e).validationEntries().get(0);
                return entry.getErrorId() == ValidationErrorId.UNKNOWN_SHAPE_MEMBER
                       && entry.getSeverity() == ValidationErrorSeverity.DANGER;
            }, "validation entry is UNKNOWN_SHAPE_MEMBER / DANGER");
    }
}
