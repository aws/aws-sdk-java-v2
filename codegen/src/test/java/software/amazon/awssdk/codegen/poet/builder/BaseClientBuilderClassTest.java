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
import static software.amazon.awssdk.codegen.poet.ClientTestModels.internalConfigModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.operationWithNoAuth;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.queryServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.queryServiceModelsEndpointAuthParamsWithAllowList;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.restJsonServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.serviceWithNoAuth;
import static software.amazon.awssdk.codegen.poet.builder.BuilderClassTestUtils.validateGeneration;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

/**
 * Validate BaseClientBuilderClass generation.
 */
public class BaseClientBuilderClassTest {
    @Test
    public void baseClientBuilderClass() {
        validateBaseClientBuilderClassGeneration(restJsonServiceModels(), "test-client-builder-class.java");
    }

    @Test
    public void baseClientBuilderClassWithBearerAuth() {
        validateBaseClientBuilderClassGeneration(bearerAuthServiceModels(), "test-bearer-auth-client-builder-class.java");
    }

    @Test
    public void baseClientBuilderClassWithNoAuthOperation() {
        validateBaseClientBuilderClassGeneration(operationWithNoAuth(), "test-no-auth-ops-client-builder-class.java");
    }

    @Test
    public void baseClientBuilderClassWithNoAuthService() {
        validateBaseClientBuilderClassGeneration(serviceWithNoAuth(), "test-no-auth-service-client-builder-class.java");
    }

    @Test
    public void baseClientBuilderClassWithInternalUserAgent() {
        validateBaseClientBuilderClassGeneration(internalConfigModels(), "test-client-builder-internal-defaults-class.java");
    }

    @Test
    public void baseQueryClientBuilderClass() {
        validateBaseClientBuilderClassGeneration(queryServiceModels(), "test-query-client-builder-class.java");
    }

    @Test
    public void baseClientBuilderClassWithEndpointsAuthParams() {
        validateBaseClientBuilderClassGeneration(queryServiceModelsEndpointAuthParamsWithAllowList(),
                                                 "test-client-builder-endpoints-auth-params.java");
    }

    @Test
    public void syncComposedDefaultClientBuilderClass() {
        validateBaseClientBuilderClassGeneration(composedClientJsonServiceModels(),
                                                 "test-composed-sync-default-client-builder.java");
    }

    private void validateBaseClientBuilderClassGeneration(IntermediateModel model, String expectedClassName) {
        validateGeneration(BaseClientBuilderClass::new, model, expectedClassName);

        model.getCustomizationConfig().setUseSraAuth(true);
        validateGeneration(BaseClientBuilderClass::new, model, "sra/" + expectedClassName);
    }
}
