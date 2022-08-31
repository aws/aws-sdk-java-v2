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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.core.rules.model.Endpoint;

public class EndpointProviderInterfaceSpec implements ClassSpec {
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public EndpointProviderInterfaceSpec(IntermediateModel intermediateModel) {
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
    }

    @Override
    public TypeSpec poetSpec() {
        return TypeSpec.interfaceBuilder(className())
                       .addAnnotation(SdkPublicApi.class)
                       .addMethod(resolveEndpointMethod())
                       .build();
    }

    private MethodSpec resolveEndpointMethod() {
        return MethodSpec.methodBuilder("resolveEndpoint")
                         .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addParameter(endpointRulesSpecUtils.parametersClassName(), "endpointParams")
            .returns(Endpoint.class)
                         .build();
    }

    @Override
    public ClassName className() {
        return endpointRulesSpecUtils.providerInterfaceName();
    }
}
