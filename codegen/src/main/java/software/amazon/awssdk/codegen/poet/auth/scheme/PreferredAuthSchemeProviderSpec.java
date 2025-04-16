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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.utils.CollectionUtils;

public class PreferredAuthSchemeProviderSpec implements ClassSpec {
    private final AuthSchemeSpecUtils authSchemeSpecUtils;
    private final AuthSchemeCodegenKnowledgeIndex knowledgeIndex;

    public PreferredAuthSchemeProviderSpec(IntermediateModel intermediateModel) {
        this.authSchemeSpecUtils = new AuthSchemeSpecUtils(intermediateModel);
        this.knowledgeIndex = AuthSchemeCodegenKnowledgeIndex.of(intermediateModel);
    }

    @Override
    public ClassName className() {
        return authSchemeSpecUtils.preferredAuthSchemeProviderName();
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createClassBuilder(className())
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addAnnotation(SdkInternalApi.class)
                        .addField(
                            authSchemeSpecUtils.providerInterfaceName(), "delegate",
                            Modifier.PRIVATE, Modifier.FINAL)
                        .addField(
                            ParameterizedTypeName.get(List.class, String.class), "authSchemePreference",
                            Modifier.PRIVATE, Modifier.FINAL)
                        .addSuperinterface(authSchemeSpecUtils.providerInterfaceName())
                        .addMethod(constructor())
                        .addMethod(resolveAuthSchemeMethod())
                        .build();
    }
    private MethodSpec constructor() {
        return MethodSpec
            .constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(authSchemeSpecUtils.providerInterfaceName(), "delegate")
            .addParameter(ParameterizedTypeName.get(List.class, String.class), "authSchemePreference")
            .addStatement("this.delegate = delegate")
            .addStatement("this.authSchemePreference = authSchemePreference")
            .build();
    }

    private MethodSpec resolveAuthSchemeMethod() {
        MethodSpec.Builder b = MethodSpec.methodBuilder("resolveAuthScheme")
                                         .addModifiers(Modifier.PUBLIC)
                                         .addAnnotation(Override.class)
                                         .returns(authSchemeSpecUtils.resolverReturnType())
                                         .addParameter(authSchemeSpecUtils.parametersInterfaceName(), "params");
        b.addJavadoc("Resolve the auth schemes based on the given set of parameters.");
        b.addStatement("$T candidateAuthSchemes = delegate.resolveAuthScheme(params)",
                       authSchemeSpecUtils.resolverReturnType());
        b.beginControlFlow("if ($T.isNullOrEmpty(authSchemePreference))", CollectionUtils.class)
         .addStatement("return candidateAuthSchemes")
         .endControlFlow();

        b.addStatement("$T authSchemes = new $T<>()", authSchemeSpecUtils.resolverReturnType(), ArrayList.class);
        b.beginControlFlow("authSchemePreference.forEach( preferredSchemeId -> ")
         .addStatement("candidateAuthSchemes.stream().filter(a -> a.schemeId().equals(preferredSchemeId)).findFirst()"
                       + ".ifPresent(a -> authSchemes.add(a))")
         .endControlFlow(")");

        b.beginControlFlow("candidateAuthSchemes.forEach(candidate -> ")
         .beginControlFlow("if (!authSchemes.contains(candidate))")
         .addStatement("authSchemes.add(candidate)")
         .endControlFlow()
         .endControlFlow(")");

        b.addStatement("return authSchemes");
        return b.build();
    }
}
