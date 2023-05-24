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
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.http.auth.spi.AuthSchemeProvider;
import software.amazon.awssdk.http.auth.spi.HttpAuthOption;

public class AuthSchemeProviderSpec implements ClassSpec {
    private final IntermediateModel intermediateModel;
    private final AuthSchemeSpecUtils authSchemeSpecUtils;

    public AuthSchemeProviderSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.authSchemeSpecUtils = new AuthSchemeSpecUtils(intermediateModel);
    }

    @Override
    public ClassName className() {
        return authSchemeSpecUtils.providerInterfaceName();
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createInterfaceBuilder(className())
                        .addSuperinterface(AuthSchemeProvider.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(SdkPublicApi.class)
                        .addJavadoc(interfaceJavadoc())
                        .addMethod(resolveAuthSchemeMethod())
                        .addMethod(resolveAuthSchemeConsumerBuilderMethod())
                        .addMethod(defaultProviderMethod())
                        .build();
    }

    private MethodSpec resolveAuthSchemeMethod() {
        MethodSpec.Builder b = MethodSpec.methodBuilder("resolveAuthScheme");
        b.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        b.addParameter(authSchemeSpecUtils.parametersInterfaceName(), "authSchemeParams");
        b.returns(authSchemeSpecUtils.resolverReturnType());
        b.addJavadoc(resolveMethodJavadoc());
        return b.build();
    }

    private MethodSpec resolveAuthSchemeConsumerBuilderMethod() {
        ClassName parametersInterface = authSchemeSpecUtils.parametersInterfaceName();
        ClassName parametersBuilderInterface = parametersInterface.nestedClass("Builder");
        TypeName consumerType = ParameterizedTypeName.get(ClassName.get(Consumer.class), parametersBuilderInterface);

        MethodSpec.Builder b = MethodSpec.methodBuilder("resolveAuthScheme");
        b.addModifiers(Modifier.PUBLIC, Modifier.DEFAULT);
        b.addParameter(consumerType, "consumer");
        b.returns(authSchemeSpecUtils.resolverReturnType());
        b.addJavadoc(resolveMethodJavadoc());

        b.addStatement("$T builder = $T.builder()", parametersBuilderInterface, parametersInterface);
        b.addStatement("consumer.accept(builder)");
        b.addStatement("return resolveAuthScheme(builder.build())");

        return b.build();
    }

    private MethodSpec defaultProviderMethod() {
        return MethodSpec.methodBuilder("defaultProvider")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(className())
            .addJavadoc("Get the default auth scheme provider.")
            .addStatement("return $T.create()", authSchemeSpecUtils.providerDefaultImplName())
            .build();
    }

    private CodeBlock interfaceJavadoc() {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("An auth scheme provider for $N service. The auth scheme provider takes a set of parameters using {@link $T}, and "
              + "resolves a list of {@link $T} based on the given parameters.",
              intermediateModel.getMetadata().getServiceName(),
              authSchemeSpecUtils.parametersInterfaceName(),
              HttpAuthOption.class);

        return b.build();
    }

    private CodeBlock resolveMethodJavadoc() {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("Resolve the auth schemes based on the given set of parameters.");

        return b.build();
    }
}
