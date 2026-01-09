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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.naming.DefaultSmithyNamingStrategy;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.smithy.model.Model;

/**
 * Builds an intermediate model to be used by the templates from the service model and
 * customizations.
 */
public class SmithyIntermediateModelBuilder {
    private static final Logger log = LoggerFactory.getLogger(SmithyIntermediateModelBuilder.class);

    private final Model smithyModel;
    private final CustomizationConfig customizationConfig;
    private final NamingStrategy namingStrategy;
    private final TypeUtils typeUtils;

    public SmithyIntermediateModelBuilder(SmithyModelWithCustomizations modelWithCustomizations) {
        this.smithyModel = modelWithCustomizations.getSmithyModel();
        this.customizationConfig = modelWithCustomizations.getCustomizationConfig();
        this.namingStrategy = new DefaultSmithyNamingStrategy(smithyModel, customizationConfig);
        this.typeUtils = new TypeUtils(namingStrategy);
    }


    public IntermediateModel build() {

        return null;
    }
}
