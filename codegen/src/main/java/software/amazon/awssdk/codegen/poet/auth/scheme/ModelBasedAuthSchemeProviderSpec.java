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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;

public class ModelBasedAuthSchemeProviderSpec implements ClassSpec {
    private final AuthSchemeSpecUtils authSchemeSpecUtils;
    private final AuthSchemeCodegenKnowledgeIndex knowledgeIndex;

    public ModelBasedAuthSchemeProviderSpec(IntermediateModel intermediateModel) {
        this.authSchemeSpecUtils = new AuthSchemeSpecUtils(intermediateModel);
        this.knowledgeIndex = AuthSchemeCodegenKnowledgeIndex.of(intermediateModel);
    }

    @Override
    public ClassName className() {
        if (authSchemeSpecUtils.useEndpointBasedAuthProvider()) {
            return authSchemeSpecUtils.modeledAuthSchemeProviderName();
        }
        return authSchemeSpecUtils.defaultAuthSchemeProviderName();
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createClassBuilder(className())
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addAnnotation(SdkInternalApi.class)
                        .addSuperinterface(authSchemeSpecUtils.providerInterfaceName())
                        .addMethod(constructor())
                        .addField(defaultInstance())
                        .addMethod(createMethod())
                        .addMethod(resolveAuthSchemeMethod())
                        .build();
    }

    private FieldSpec defaultInstance() {
        return FieldSpec.builder(className(), "DEFAULT")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T()", className())
                        .build();
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build();
    }

    private MethodSpec createMethod() {
        return MethodSpec.methodBuilder("create")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .returns(className())
                         .addStatement("return DEFAULT")
                         .build();
    }

    private MethodSpec resolveAuthSchemeMethod() {
        MethodSpec.Builder spec = MethodSpec.methodBuilder("resolveAuthScheme")
                                            .addModifiers(Modifier.PUBLIC)
                                            .addAnnotation(Override.class)
                                            .returns(authSchemeSpecUtils.resolverReturnType())
                                            .addParameter(authSchemeSpecUtils.parametersInterfaceName(), "params");

        spec.addStatement("$T options = new $T<>()", ParameterizedTypeName.get(List.class, AuthSchemeOption.class),
                          TypeName.get(ArrayList.class));

        if (knowledgeIndex.hasPerOperationAuthSchemesOverrides()) {
            // We create a switch to return the auth schemes overrides per
            // operation.
            spec.beginControlFlow("switch(params.operation())");
            knowledgeIndex.forEachOperationsOverridesGroup((ops, schemes) -> addCasesForOperations(spec, ops, schemes));
            addCasesForOperations(spec, Collections.emptyList(), knowledgeIndex.serviceDefaultAuthSchemes());
            spec.endControlFlow();
        } else {
            // All the operations share the same set of auth schemes, no need to create a switch statement.
            List<AuthSchemeCodegenMetadata> types = knowledgeIndex.serviceDefaultAuthSchemes();
            for (AuthSchemeCodegenMetadata authType : types) {
                addAuthTypeProperties(spec, authType);
            }
        }
        return spec.addStatement("return $T.unmodifiableList(options)", Collections.class)
                   .build();
    }

    private void addCasesForOperations(MethodSpec.Builder spec, List<String> operations,
                                       List<AuthSchemeCodegenMetadata> schemes) {
        if (operations.isEmpty()) {
            spec.addCode("default:\n");
        } else {
            for (String name : operations) {
                spec.addCode("case $S\n:", name);
            }
        }
        for (AuthSchemeCodegenMetadata metadata : schemes) {
            addAuthTypeProperties(spec, metadata);
        }
        spec.addStatement("break");
    }

    private void addAuthTypeProperties(MethodSpec.Builder spec, AuthSchemeCodegenMetadata metadata) {
        spec.addCode("options.add(");
        spec.addCode(AuthSchemeCodegenMetadataExt.codegenNewAuthOption(metadata, authSchemeSpecUtils));
        spec.addCode(".build()");
        spec.addCode(");\n");
    }
}
