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

package software.amazon.awssdk.codegen.validation;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.config.customization.UnderscoresInNameBehavior;

public class CustomizationConfigValidator implements ModelValidator{
    @Override
    public List<ValidationEntry> validateModels(ModelValidationContext context) {
        CustomizationConfig config = context.intermediateModel().getCustomizationConfig();

        if (config.getUnderscoresInNameBehavior() == UnderscoresInNameBehavior.ALLOW &&
            config.getAllowedUnderscoreNames() != null &&
            !config.getAllowedUnderscoreNames().isEmpty()) {

            return Collections.singletonList(
                ValidationEntry.create(
                    ValidationErrorId.INVALID_CODEGEN_CUSTOMIZATION,
                    ValidationErrorSeverity.DANGER,
                    "Cannot set both 'underscoresInNameBehavior=ALLOW' and 'allowedUnderscoreNames'. " +
                    "Use 'allowedUnderscoreNames' for granular control or 'underscoresInNameBehavior=ALLOW' for all underscores."
                )
            );
        }

        return Collections.emptyList();
    }

}
