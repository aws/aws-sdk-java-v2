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

package software.amazon.awssdk.codegen.poet.rules;

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

class EndpointProviderCompiledRulesClassSpecTest {

    @Test
    public void endpointProviderClass() {
        ClassSpec endpointProviderSpec = new EndpointProviderSpec(ClientTestModels.queryServiceModels());
        assertThat(endpointProviderSpec, generatesTo("endpoint-provider-class.java"));
    }

    @Test
    void knowPropertiesOverride() {
        ClassSpec endpointProviderSpec =
            new EndpointProviderSpec(ClientTestModels.queryServiceModelsWithOverrideKnowProperties());
        assertThat(endpointProviderSpec, generatesTo("endpoint-provider-know-prop-override-class.java"));
    }
}
