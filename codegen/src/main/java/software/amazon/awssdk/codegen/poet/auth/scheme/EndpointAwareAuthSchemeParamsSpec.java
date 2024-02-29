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

package software.amazon.awssdk.codegen.poet.auth.scheme;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;

public class EndpointAwareAuthSchemeParamsSpec implements ClassSpec {

    private final AuthSchemeSpecUtils authSchemeSpecUtils;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public EndpointAwareAuthSchemeParamsSpec(IntermediateModel intermediateModel) {
        this.authSchemeSpecUtils = new AuthSchemeSpecUtils(intermediateModel);
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
    }

    @Override
    public ClassName className() {
        return authSchemeSpecUtils.parametersEndpointAwareDefaultImplName();
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder b = PoetUtils.createInterfaceBuilder(className())
                                      .addAnnotation(SdkInternalApi.class)
                                      .addMethod(endpointProviderMethod())
                                      .addType(builderSpec());
        return b.build();
    }

    private ClassName builderClassName() {
        return className().nestedClass("Builder");
    }

    private MethodSpec endpointProviderMethod() {
        return MethodSpec.methodBuilder("endpointProvider")
                         .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                         .returns(endpointRulesSpecUtils.providerInterfaceName())
                         .build();
    }

    private TypeSpec builderSpec() {
        ClassName builderClassName = builderClassName();
        TypeSpec.Builder b = TypeSpec.interfaceBuilder(builderClassName)
                                     .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        b.addMethod(MethodSpec.methodBuilder("endpointProvider")
                              .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                              .addParameter(endpointRulesSpecUtils.providerInterfaceName(), "endpointProvider")
                              .returns(builderClassName)
                              .build());

        return b.build();
    }
}
