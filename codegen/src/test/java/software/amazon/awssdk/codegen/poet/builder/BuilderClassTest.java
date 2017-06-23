/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import org.junit.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

/**
 * Validate client builder generation.
 */
public class BuilderClassTest {
    @Test
    public void baseClientBuilderInterface() throws Exception {
        validateGeneration(BaseClientBuilderInterface::new, "test-client-builder-interface.java");
    }

    @Test
    public void baseClientBuilderClass() throws Exception {
        validateGeneration(BaseClientBuilderClass::new, "test-client-builder-class.java");
    }

    @Test
    public void syncClientBuilderInterface() throws Exception {
        validateGeneration(SyncClientBuilderInterface::new, "test-sync-client-builder-interface.java");
    }

    @Test
    public void syncClientBuilderClass() throws Exception {
        validateGeneration(SyncClientBuilderClass::new, "test-sync-client-builder-class.java");
    }

    @Test
    public void asyncClientBuilderInterface() throws Exception {
        validateGeneration(AsyncClientBuilderInterface::new, "test-async-client-builder-interface.java");
    }

    @Test
    public void asyncClientBuilderClass() throws Exception {
        validateGeneration(AsyncClientBuilderClass::new, "test-async-client-builder-class.java");
    }

    private void validateGeneration(Function<IntermediateModel, ClassSpec> generatorConstructor, String expectedClassName) {
        assertThat(generatorConstructor.apply(ClientTestModels.jsonServiceModels()), generatesTo(expectedClassName));
    }
}
