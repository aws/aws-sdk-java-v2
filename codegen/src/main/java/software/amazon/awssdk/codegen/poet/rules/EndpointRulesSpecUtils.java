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

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.jr.stree.JrsBoolean;
import com.fasterxml.jackson.jr.stree.JrsString;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.rules.endpoints.BuiltInParameter;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

public class EndpointRulesSpecUtils {
    private final IntermediateModel intermediateModel;

    public EndpointRulesSpecUtils(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
    }

    public String basePackage() {
        return intermediateModel.getMetadata().getFullEndpointRulesPackageName();
    }

    public ClassName rulesRuntimeClassName(String name) {
        return ClassName.get(intermediateModel.getMetadata().getFullInternalEndpointRulesPackageName(),
                             name);
    }

    public ClassName parametersClassName() {
        return ClassName.get(basePackage(), intermediateModel.getMetadata().getServiceName() + "EndpointParams");
    }

    public ClassName providerInterfaceName() {
        return ClassName.get(basePackage(), intermediateModel.getMetadata().getServiceName() + "EndpointProvider");
    }

    public ClassName providerDefaultImplName() {
        Metadata md = intermediateModel.getMetadata();
        return ClassName.get(md.getFullInternalEndpointRulesPackageName(),
                             "Default" + providerInterfaceName().simpleName());
    }

    public ClassName resolverInterceptorName() {
        Metadata md = intermediateModel.getMetadata();
        return ClassName.get(md.getFullInternalEndpointRulesPackageName(),
                             md.getServiceName() + "ResolveEndpointInterceptor");
    }

    public ClassName authSchemesInterceptorName() {
        Metadata md = intermediateModel.getMetadata();
        return ClassName.get(md.getFullInternalEndpointRulesPackageName(),
                             md.getServiceName() + "EndpointAuthSchemeInterceptor");
    }

    public ClassName requestModifierInterceptorName() {
        Metadata md = intermediateModel.getMetadata();
        return ClassName.get(md.getFullInternalEndpointRulesPackageName(),
                             md.getServiceName() + "RequestSetEndpointInterceptor");
    }

    public ClassName clientEndpointTestsName() {
        Metadata md = intermediateModel.getMetadata();
        return ClassName.get(md.getFullEndpointRulesPackageName(),
                             md.getServiceName() + "ClientEndpointTests");
    }

    public ClassName endpointProviderTestsName() {
        Metadata md = intermediateModel.getMetadata();
        return ClassName.get(md.getFullEndpointRulesPackageName(),
                             md.getServiceName() + "EndpointProviderTests");
    }

    public ClassName clientContextParamsName() {
        Metadata md = intermediateModel.getMetadata();
        return ClassName.get(md.getFullEndpointRulesPackageName(),
                             md.getServiceName() + "ClientContextParams");
    }

    public String paramMethodName(String param) {
        return Utils.unCapitalize(CodegenNamingUtils.pascalCase(param));
    }

    public String clientContextParamMethodName(String param) {
        return Utils.unCapitalize(CodegenNamingUtils.pascalCase(param));
    }

    public String clientContextParamName(String paramName) {
        return intermediateModel.getNamingStrategy().getEnumValueName(paramName);
    }

    public static TypeName toJavaType(String type) {
        switch (type.toLowerCase(Locale.ENGLISH)) {
            case "boolean":
                return TypeName.get(Boolean.class);
            case "string":
                return TypeName.get(String.class);
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }

    public CodeBlock valueCreationCode(String type, CodeBlock param) {
        String methodName;
        switch (type.toLowerCase(Locale.ENGLISH)) {
            case "boolean":
                methodName = "fromBool";
                break;
            case "string":
                methodName = "fromStr";
                break;
            default:
                throw new RuntimeException("Don't know how to create a Value instance from type " + type);
        }

        return CodeBlock.builder()
                        .add("$T.$N($L)", rulesRuntimeClassName("Value"), methodName, param)
                        .build();
    }

    public static TypeName parameterType(ParameterModel param) {
        if (param.getBuiltInEnum() == null || param.getBuiltInEnum() != BuiltInParameter.AWS_REGION) {
            return toJavaType(param.getType());
        }

        if (param.getBuiltInEnum() == BuiltInParameter.AWS_REGION) {
            return ClassName.get(Region.class);
        }
        return toJavaType(param.getType());
    }

    public CodeBlock treeNodeToLiteral(TreeNode treeNode) {
        CodeBlock.Builder b = CodeBlock.builder();

        switch (treeNode.asToken()) {
            case VALUE_STRING:
                b.add("$S", Validate.isInstanceOf(JrsString.class, treeNode, "Expected string").getValue());
                break;
            case VALUE_TRUE:
            case VALUE_FALSE:
                b.add("$L", Validate.isInstanceOf(JrsBoolean.class, treeNode, "Expected boolean").booleanValue());
                break;
            default:
                throw new RuntimeException("Don't know how to set default value for parameter of type "
                                           + treeNode.asToken());
        }
        return b.build();
    }


    public boolean isS3() {
        return "S3".equals(intermediateModel.getMetadata().getServiceName());
    }

    public boolean isS3Control() {
        return "S3Control".equals(intermediateModel.getMetadata().getServiceName());
    }

    public TypeName resolverReturnType() {
        return ParameterizedTypeName.get(CompletableFuture.class, Endpoint.class);
    }

    public List<String> rulesEngineResourceFiles() {
        URL currentJarUrl = EndpointRulesSpecUtils.class.getProtectionDomain().getCodeSource().getLocation();
        try (JarFile jarFile = new JarFile(currentJarUrl.getFile())) {
            return jarFile.stream()
                          .map(ZipEntry::getName)
                          .filter(e -> e.startsWith("software/amazon/awssdk/codegen/rules"))
                          .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public boolean isDeclaredParam(String paramName) {
        Map<String, ParameterModel> parameters = intermediateModel.getEndpointRuleSetModel().getParameters();
        return parameters.containsKey(paramName);
    }

    public FieldSpec parameterClassField(String name, ParameterModel model) {
        return parameterFieldSpecBuilder(name, model)
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.FINAL)
            .build();
    }

    public MethodSpec parameterClassAccessorMethod(String name, ParameterModel model) {
        MethodSpec.Builder b = parameterMethodBuilder(name, model);
        b.returns(parameterType(model));
        b.addStatement("return $N", variableName(name));
        return b.build();
    }

    public MethodSpec parameterInterfaceAccessorMethod(String name, ParameterModel model) {
        MethodSpec.Builder b = parameterMethodBuilder(name, model);
        b.returns(parameterType(model));
        b.addModifiers(Modifier.ABSTRACT);
        return b.build();
    }

    public FieldSpec parameterBuilderFieldSpec(String name, ParameterModel model) {
        return parameterFieldSpecBuilder(name, model)
            .initializer(parameterDefaultValueCode(model))
            .build();
    }

    public FieldSpec.Builder parameterFieldSpecBuilder(String name, ParameterModel model) {
        return FieldSpec.builder(parameterType(model), variableName(name))
                        .addModifiers(Modifier.PRIVATE);
    }

    public MethodSpec parameterBuilderSetterMethodDeclaration(ClassName containingClass, String name, ParameterModel model) {
        MethodSpec.Builder b = parameterMethodBuilder(name, model);
        b.addModifiers(Modifier.ABSTRACT);
        b.addParameter(parameterSpec(name, model));
        b.returns(containingClass.nestedClass("Builder"));
        return b.build();
    }

    public MethodSpec parameterBuilderSetterMethod(ClassName containingClass, String name, ParameterModel model) {
        String memberName = variableName(name);

        MethodSpec.Builder b = parameterMethodBuilder(name, model)
            .addAnnotation(Override.class)
            .addParameter(parameterSpec(name, model))
            .returns(containingClass.nestedClass("Builder"))
            .addStatement("this.$1N = $1N", memberName);

        TreeNode defaultValue = model.getDefault();
        if (defaultValue != null) {
            b.beginControlFlow("if (this.$N == null)", memberName);
            b.addStatement("this.$N = $L", memberName, parameterDefaultValueCode(model));
            b.endControlFlow();
        }

        b.addStatement("return this");
        return b.build();
    }

    private ParameterSpec parameterSpec(String name, ParameterModel model) {
        return ParameterSpec.builder(parameterType(model), variableName(name)).build();
    }

    private String variableName(String name) {
        return intermediateModel.getNamingStrategy().getVariableName(name);
    }

    private MethodSpec.Builder parameterMethodBuilder(String name, ParameterModel model) {
        MethodSpec.Builder b = MethodSpec.methodBuilder(paramMethodName(name));
        b.addModifiers(Modifier.PUBLIC);
        if (model.getDeprecated() != null) {
            b.addAnnotation(Deprecated.class);
        }
        return b;
    }

    private CodeBlock parameterDefaultValueCode(ParameterModel parameterModel) {
        CodeBlock.Builder b = CodeBlock.builder();

        TreeNode defaultValue = parameterModel.getDefault();

        if (defaultValue == null) {
            return b.build();
        }

        switch (defaultValue.asToken()) {
            case VALUE_STRING:
                String stringValue = ((JrsString) defaultValue).getValue();
                if (parameterModel.getBuiltInEnum() == BuiltInParameter.AWS_REGION) {
                    b.add("$T.of($S)", Region.class, stringValue);
                } else {
                    b.add("$S", stringValue);
                }
                break;
            case VALUE_TRUE:
            case VALUE_FALSE:
                b.add("$L", ((JrsBoolean) defaultValue).booleanValue());
                break;
            default:
                throw new RuntimeException("Don't know how to set default value for parameter of type "
                                           + defaultValue.asToken());
        }
        return b.build();
    }
}
