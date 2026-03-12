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

package software.amazon.awssdk.codegen.poet.builder;

import static software.amazon.awssdk.codegen.poet.ClientTestModels.bearerAuthServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.composedClientJsonServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.opsWithSigv4a;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.queryServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.restJsonServiceModels;
import static software.amazon.awssdk.codegen.poet.builder.BuilderClassTestUtils.validateGeneration;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

/**
 * Validate BaseClientBuilderInterface generation.
 */
public class BaseClientBuilderInterfaceTest {
    @Test
    public void baseClientBuilderInterface() {
        validateBaseClientBuilderInterfaceGeneration(restJsonServiceModels(), "test-client-builder-interface.java");
    }

    @Test
    public void baseClientBuilderInterfaceWithBearerAuth() {
        validateBaseClientBuilderInterfaceGeneration(bearerAuthServiceModels(), "test-bearer-auth-client-builder-interface.java");
    }

    @Test
    public void baseClientBuilderInterfaceWithEndpointParams() {
        validateBaseClientBuilderInterfaceGeneration(queryServiceModels(), "test-endpointparams-client-builder-interface.java");
    }

    @Test
    public void syncHasCrossRegionAccessEnabledPropertyBuilderClass() {
        validateBaseClientBuilderInterfaceGeneration(composedClientJsonServiceModels(),
                                                     "test-customcontextparams-sync-client-builder-class.java");
    }

    @Test
    void baseClientBuilderInterfaceWithMultiAuth() {
        validateBaseClientBuilderInterfaceGeneration(opsWithSigv4a(), "test-multi-auth-sigv4a-client-builder-interface.java");
    }

    private void validateBaseClientBuilderInterfaceGeneration(IntermediateModel model, String expectedClassName) {
        validateGeneration(BaseClientBuilderInterface::new, model, expectedClassName);
    }
}
