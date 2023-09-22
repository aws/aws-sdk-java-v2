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

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.bearerAuthServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.composedClientJsonServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.internalConfigModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.operationWithNoAuth;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.queryServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.queryServiceModelsEndpointAuthParamsWithAllowList;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.restJsonServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.serviceWithNoAuth;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.util.function.Function;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

/**
 * Validate client builder generation.
 */
public class BuilderClassTest {
    @Test
    public void baseClientBuilderInterface() {
        validateBaseClientBuilderInterfaceGeneration(restJsonServiceModels(), "test-client-builder-interface.java");
    }

    @Test
    public void baseClientBuilderClass() {
        validateBaseClientBuilderClassGeneration(restJsonServiceModels(), "test-client-builder-class.java");
    }

    @Test
    public void baseClientBuilderInterfaceWithBearerAuth() {
        validateBaseClientBuilderInterfaceGeneration(bearerAuthServiceModels(), "test-bearer-auth-client-builder-interface.java");
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
    public void syncClientBuilderInterface() {
        validateGeneration(SyncClientBuilderInterface::new, restJsonServiceModels(), "test-sync-client-builder-interface.java");
    }

    @Test
    public void syncClientBuilderClass() {
        validateGeneration(SyncClientBuilderClass::new, restJsonServiceModels(), "test-sync-client-builder-class.java");
    }

    @Test
    public void syncComposedClientBuilderClass() {
        validateGeneration(SyncClientBuilderClass::new, composedClientJsonServiceModels(),
                           "test-composed-sync-client-builder-class.java");
    }

    @Test
    public void syncComposedDefaultClientBuilderClass() {
        validateBaseClientBuilderClassGeneration(composedClientJsonServiceModels(),
                                                 "test-composed-sync-default-client-builder.java");
    }

    @Test
    public void syncHasCrossRegionAccessEnabledPropertyBuilderClass() {
        validateBaseClientBuilderInterfaceGeneration(composedClientJsonServiceModels(),
                                                     "test-customcontextparams-sync-client-builder-class.java");
    }


    @Test
    public void asyncClientBuilderInterface() {
        validateGeneration(AsyncClientBuilderInterface::new, restJsonServiceModels(), "test-async-client-builder-interface.java");
    }

    @Test
    public void asyncClientBuilderClass() {
        validateGeneration(AsyncClientBuilderClass::new, restJsonServiceModels(), "test-async-client-builder-class.java");
    }

    @Test
    public void asyncComposedClientBuilderClass() {
        validateGeneration(AsyncClientBuilderClass::new, composedClientJsonServiceModels(),
                           "test-composed-async-client-builder-class.java");
    }

    private void validateBaseClientBuilderInterfaceGeneration(IntermediateModel model, String expectedClassName) {
        validateGeneration(BaseClientBuilderInterface::new, model, expectedClassName);
    }

    private void validateBaseClientBuilderClassGeneration(IntermediateModel model, String expectedClassName) {
        validateGeneration(BaseClientBuilderClass::new, model, expectedClassName);
    }

    private void validateGeneration(Function<IntermediateModel, ClassSpec> generatorConstructor,
                                    IntermediateModel model,
                                    String expectedClassName) {
        assertThat(generatorConstructor.apply(model), generatesTo(expectedClassName));
    }
}
