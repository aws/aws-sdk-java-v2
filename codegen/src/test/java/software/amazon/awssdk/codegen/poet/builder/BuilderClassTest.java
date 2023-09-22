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
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.util.function.Function;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

/**
 * Validate client builder generation.
 */
public class BuilderClassTest {
    @Test
    public void baseClientBuilderInterface() {
        validateGeneration(BaseClientBuilderInterface::new, "test-client-builder-interface.java");
    }

    @Test
    public void baseClientBuilderClass() {
        validateGeneration(BaseClientBuilderClass::new, "test-client-builder-class.java");
    }

    @Test
    public void baseClientBuilderInterfaceWithBearerAuth() {
        validateBearerAuthGeneration(BaseClientBuilderInterface::new, "test-bearer-auth-client-builder-interface.java");
    }

    @Test
    public void baseClientBuilderClassWithBearerAuth() {
        validateBearerAuthGeneration(BaseClientBuilderClass::new, "test-bearer-auth-client-builder-class.java");
    }

    @Test
    public void baseClientBuilderClassWithNoAuthOperation() {
        validateNoAuthOperationAuthGeneration(BaseClientBuilderClass::new, "test-no-auth-ops-client-builder-class.java");
    }

    @Test
    public void baseClientBuilderClassWithNoAuthService() {
        validateNoAuthServiceAuthGeneration(BaseClientBuilderClass::new, "test-no-auth-service-client-builder-class.java");
    }

    @Test
    public void baseClientBuilderClassWithInternalUserAgent() {
        assertThat(new BaseClientBuilderClass(ClientTestModels.internalConfigModels()), generatesTo("test-client-builder-internal-defaults-class.java"));
    }

    @Test
    public void baseQueryClientBuilderClass() {
        validateQueryGeneration(BaseClientBuilderClass::new, "test-query-client-builder-class.java");
    }

    @Test
    public void baseClientBuilderClassWithEndpointsAuthParams() {
        assertThat(new BaseClientBuilderClass(ClientTestModels.queryServiceModelsEndpointAuthParamsWithAllowList()), generatesTo("test-client-builder-endpoints-auth-params.java"));
    }

    @Test
    public void syncClientBuilderInterface() {
        validateGeneration(SyncClientBuilderInterface::new, "test-sync-client-builder-interface.java");
    }

    @Test
    public void syncClientBuilderClass() {
        validateGeneration(SyncClientBuilderClass::new, "test-sync-client-builder-class.java");
    }

    @Test
    public void syncComposedClientBuilderClass() {
        validateComposedClientGeneration(SyncClientBuilderClass::new, "test-composed-sync-client-builder-class.java");
    }

    @Test
    public void syncComposedDefaultClientBuilderClass() {
        validateComposedClientGeneration(BaseClientBuilderClass::new, "test-composed-sync-default-client-builder.java");
    }

    @Test
    public void syncHasCrossRegionAccessEnabledPropertyBuilderClass() {
        validateComposedClientGeneration(BaseClientBuilderInterface::new, "test-customcontextparams-sync-client-builder-class.java");
    }


    @Test
    public void asyncClientBuilderInterface() {
        validateGeneration(AsyncClientBuilderInterface::new, "test-async-client-builder-interface.java");
    }

    @Test
    public void asyncClientBuilderClass() {
        validateGeneration(AsyncClientBuilderClass::new, "test-async-client-builder-class.java");
    }

    @Test
    public void asyncComposedClientBuilderClass() {
        validateComposedClientGeneration(AsyncClientBuilderClass::new, "test-composed-async-client-builder-class.java");
    }

    private void validateGeneration(Function<IntermediateModel, ClassSpec> generatorConstructor, String expectedClassName) {
        assertThat(generatorConstructor.apply(ClientTestModels.restJsonServiceModels()), generatesTo(expectedClassName));
    }

    private void validateComposedClientGeneration(Function<IntermediateModel, ClassSpec> generatorConstructor,
                                                  String expectedClassName) {
        assertThat(generatorConstructor.apply(ClientTestModels.composedClientJsonServiceModels()), generatesTo(expectedClassName));
    }

    private void validateBearerAuthGeneration(Function<IntermediateModel, ClassSpec> generatorConstructor,
                                           String expectedClassName) {
        assertThat(generatorConstructor.apply(ClientTestModels.bearerAuthServiceModels()), generatesTo(expectedClassName));
    }

    private void validateNoAuthOperationAuthGeneration(Function<IntermediateModel, ClassSpec> generatorConstructor,
                                                       String expectedClassName) {
        assertThat(generatorConstructor.apply(ClientTestModels.operationWithNoAuth()), generatesTo(expectedClassName));
    }

    private void validateNoAuthServiceAuthGeneration(Function<IntermediateModel, ClassSpec> generatorConstructor,
                                                     String expectedClassName) {
        assertThat(generatorConstructor.apply(ClientTestModels.serviceWithNoAuth()), generatesTo(expectedClassName));
    }

    private void validateQueryGeneration(Function<IntermediateModel, ClassSpec> generatorConstructor, String expectedClassName) {
        assertThat(generatorConstructor.apply(ClientTestModels.queryServiceModels()), generatesTo(expectedClassName));
    }
}
