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
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public class DefaultAuthSchemeProviderSpec implements ClassSpec {
    private final AuthSchemeSpecUtils authSchemeSpecUtils;

    public DefaultAuthSchemeProviderSpec(IntermediateModel intermediateModel) {
        this.authSchemeSpecUtils = new AuthSchemeSpecUtils(intermediateModel);
    }

    @Override
    public ClassName className() {
        return authSchemeSpecUtils.providerDefaultImplName();
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
        return MethodSpec.methodBuilder("resolveAuthScheme")
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Override.class)
                         .returns(authSchemeSpecUtils.resolverReturnType())
                         .addParameter(authSchemeSpecUtils.parametersInterfaceName(), "authSchemeParams")
                         // TODO: make real implementation
                         .addStatement("return new $T<>()", ArrayList.class)
                         .build();
    }
}
