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
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Map;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Validate;

public class DefaultAuthSchemeParamsSpec implements ClassSpec {

    private final IntermediateModel intermediateModel;
    private final AuthSchemeSpecUtils authSchemeSpecUtils;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public DefaultAuthSchemeParamsSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.authSchemeSpecUtils = new AuthSchemeSpecUtils(intermediateModel);
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
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

        if (authSchemeSpecUtils.useEndpointBasedAuthProvider()) {
            b.addSuperinterface(authSchemeSpecUtils.parametersEndpointAwareDefaultImplName());
        }

        addFieldsAndAccessors(b);
        addToBuilder(b);
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

        if (authSchemeSpecUtils.generateEndpointBasedParams()) {
            parameters().forEach((name, model) -> {
                if (authSchemeSpecUtils.includeParam(name)) {
                    String fieldName = authSchemeSpecUtils.paramMethodName(name);
                    boolean isRequired = isParamRequired(model);
                    if (isRequired) {
                        b.addStatement("this.$1N = $2T.paramNotNull(builder.$1N, $1S)",
                                       fieldName, Validate.class);
                    } else {
                        b.addStatement("this.$1N = builder.$1N", fieldName);
                    }

                }
            });
            b.addStatement("this.endpointProvider = builder.endpointProvider");
        }

        return b.build();
    }

    private boolean isParamRequired(ParameterModel model) {
        Boolean isRequired = model.isRequired();
        return (isRequired != null && isRequired) || model.getDefault() != null;
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

        if (authSchemeSpecUtils.useEndpointBasedAuthProvider()) {
            b.addSuperinterface(authSchemeSpecUtils.parametersEndpointAwareDefaultImplName().nestedClass("Builder"));
        }

        addBuilderConstructors(b);
        addBuilderFieldsAndSetter(b);

        b.addMethod(MethodSpec.methodBuilder("build")
                              .addModifiers(Modifier.PUBLIC)
                              .addAnnotation(Override.class)
                              .returns(authSchemeSpecUtils.parametersInterfaceName())
                              .addStatement("return new $T(this)", className())
                              .build());

        return b.build();
    }

    private void addBuilderConstructors(TypeSpec.Builder b) {
        b.addMethod(MethodSpec.constructorBuilder()
                              .build());

        MethodSpec.Builder builderFromInstance = MethodSpec.constructorBuilder()
                                                           .addParameter(className(), "params");

        builderFromInstance.addStatement("this.operation = params.operation");
        if (authSchemeSpecUtils.usesSigV4()) {
            builderFromInstance.addStatement("this.region = params.region");
        }
        if (authSchemeSpecUtils.generateEndpointBasedParams()) {
            parameters().forEach((name, model) -> {
                if (authSchemeSpecUtils.includeParam(name)) {
                    builderFromInstance.addStatement("this.$1N = params.$1N", endpointRulesSpecUtils.variableName(name));
                }
            });
            builderFromInstance.addStatement("this.endpointProvider = params.endpointProvider");
        }
        b.addMethod(builderFromInstance.build());
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
            b.addField(FieldSpec.builder(Region.class, "region")
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build());

            b.addMethod(MethodSpec.methodBuilder("region")
                                  .addModifiers(Modifier.PUBLIC)
                                  .addAnnotation(Override.class)
                                  .returns(Region.class)
                                  .addStatement("return region")
                                  .build());
        }

        if (authSchemeSpecUtils.generateEndpointBasedParams()) {
            parameters().forEach((name, model) -> {
                if (authSchemeSpecUtils.includeParam(name)) {
                    b.addField(endpointRulesSpecUtils.parameterClassField(name, model));
                    b.addMethods(endpointRulesSpecUtils.parameterClassAccessorMethods(name, model)
                                                       .stream()
                                                       .map(m -> m.toBuilder()
                                                                  .addAnnotation(Override.class)
                                                                  .build())
                                                       .collect(Collectors.toList()));
                }
            });
            ClassName endpointProvider = endpointRulesSpecUtils.providerInterfaceName();
            b.addField(FieldSpec.builder(endpointProvider, "endpointProvider")
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build());
            b.addMethod(MethodSpec.methodBuilder("endpointProvider")
                                  .addModifiers(Modifier.PUBLIC)
                                  .addAnnotation(Override.class)
                                  .returns(endpointProvider)
                                  .addStatement("return endpointProvider")
                                  .build());

        }
    }

    private void addToBuilder(TypeSpec.Builder b) {
        b.addMethod(MethodSpec.methodBuilder("toBuilder")
                              .addModifiers(Modifier.PUBLIC)
                              .addAnnotation(Override.class)
                              .returns(authSchemeSpecUtils.parametersInterfaceBuilderInterfaceName())
                              .addStatement("return new $T(this)", builderClassName())
                              .build());
    }

    private void addBuilderFieldsAndSetter(TypeSpec.Builder b) {
        b.addField(FieldSpec.builder(String.class, "operation")
                            .addModifiers(Modifier.PRIVATE)
                            .build());
        b.addMethod(builderSetterMethod("operation", TypeName.get(String.class)));

        if (authSchemeSpecUtils.usesSigV4()) {
            b.addField(FieldSpec.builder(Region.class, "region")
                                .addModifiers(Modifier.PRIVATE)
                                .build());
            b.addMethod(builderSetterMethod("region", TypeName.get(Region.class)));
        }

        if (authSchemeSpecUtils.generateEndpointBasedParams()) {
            parameters().forEach((name, model) -> {
                if (authSchemeSpecUtils.includeParam(name)) {
                    b.addField(endpointRulesSpecUtils.parameterBuilderFieldSpec(name, model));
                    b.addMethod(endpointRulesSpecUtils.parameterBuilderSetterMethod(className(), name, model));
                }
            });
            b.addField(FieldSpec.builder(endpointRulesSpecUtils.providerInterfaceName(), "endpointProvider")
                                .addModifiers(Modifier.PRIVATE)
                                .build());
            b.addMethod(builderSetterMethod("endpointProvider", endpointRulesSpecUtils.providerInterfaceName()));
        }
    }

    private MethodSpec builderSetterMethod(String field, TypeName type) {
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

    private Map<String, ParameterModel> parameters() {
        return intermediateModel.getEndpointRuleSetModel().getParameters();
    }
}
