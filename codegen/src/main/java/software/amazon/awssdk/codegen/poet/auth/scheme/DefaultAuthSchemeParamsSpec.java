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
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.lang.reflect.Type;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.utils.Validate;

public class DefaultAuthSchemeParamsSpec implements ClassSpec {
    private final AuthSchemeSpecUtils authSchemeSpecUtils;

    public DefaultAuthSchemeParamsSpec(IntermediateModel intermediateModel) {
        this.authSchemeSpecUtils = new AuthSchemeSpecUtils(intermediateModel);
    }

    @Override
    public ClassName className() {
        return authSchemeSpecUtils.parametersDefaultImplName();
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder b = PoetUtils.createClassBuilder(className())
                              .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                              .addAnnotation(SdkInternalApi.class)
                              .addSuperinterface(authSchemeSpecUtils.parametersInterfaceName())
                              .addMethod(constructor())
                              .addMethod(builderMethod())
                              .addType(builderImplSpec());

        addFieldsAndAccessors(b);

        return b.build();
    }

    private MethodSpec constructor() {
        MethodSpec.Builder b = MethodSpec.constructorBuilder()
                                         .addModifiers(Modifier.PRIVATE)
                                         .addParameter(builderClassName(), "builder")
                                         .addStatement("this.operation = $T.paramNotNull(builder.operation, \"operation\")",
                                                       Validate.class);

        if (authSchemeSpecUtils.usesSigV4()) {
            b.addStatement("this.region = builder.region");
        }
        return b.build();
    }

    private MethodSpec builderMethod() {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .returns(authSchemeSpecUtils.parametersInterfaceBuilderInterfaceName())
                         .addStatement("return new $T()", builderClassName())
                         .build();
    }

    private TypeSpec builderImplSpec() {
        TypeSpec.Builder b = TypeSpec.classBuilder(builderClassName())
                                     .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                     .addSuperinterface(authSchemeSpecUtils.parametersInterfaceBuilderInterfaceName());

        addBuilderFieldsAndSetter(b);

        b.addMethod(MethodSpec.methodBuilder("build")
                              .addModifiers(Modifier.PUBLIC)
                              .addAnnotation(Override.class)
                              .returns(authSchemeSpecUtils.parametersInterfaceName())
                              .addStatement("return new $T(this)", className())
                              .build());

        return b.build();
    }

    private void addFieldsAndAccessors(TypeSpec.Builder b) {
        b.addField(FieldSpec.builder(String.class, "operation")
                            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                            .build());

        b.addMethod(MethodSpec.methodBuilder("operation")
                              .addModifiers(Modifier.PUBLIC)
                              .addAnnotation(Override.class)
                              .returns(String.class)
                              .addStatement("return operation")
                              .build());

        if (authSchemeSpecUtils.usesSigV4()) {
            b.addField(FieldSpec.builder(String.class, "region")
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build());

            b.addMethod(MethodSpec.methodBuilder("region")
                                  .addModifiers(Modifier.PUBLIC)
                                  .addAnnotation(Override.class)
                                  .returns(ParameterizedTypeName.get(Optional.class, String.class))
                                  .addStatement("return region == null ? Optional.empty() : Optional.of(region)")
                                  .build());
        }
    }

    private void addBuilderFieldsAndSetter(TypeSpec.Builder b) {
        b.addField(FieldSpec.builder(String.class, "operation")
                            .addModifiers(Modifier.PRIVATE)
                            .build());
        b.addMethod(builderSetterMethod("operation", String.class));

        if (authSchemeSpecUtils.usesSigV4()) {
            b.addField(FieldSpec.builder(String.class, "region")
                                .addModifiers(Modifier.PRIVATE)
                                .build());
            b.addMethod(builderSetterMethod("region", String.class));
        }
    }

    private MethodSpec builderSetterMethod(String field, Type type) {
        return MethodSpec.methodBuilder(field)
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Override.class)
                         .addParameter(ParameterSpec.builder(type, field).build())
                         .returns(builderClassName())
                         .addStatement("this.$L = $L", field, field)
                         .addStatement("return this")
                         .build();
    }

    private ClassName builderClassName() {
        return className().nestedClass("Builder");
    }
}
