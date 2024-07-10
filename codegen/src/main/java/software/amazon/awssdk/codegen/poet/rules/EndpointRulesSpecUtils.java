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
import com.fasterxml.jackson.jr.stree.JrsArray;
import com.fasterxml.jackson.jr.stree.JrsBoolean;
import com.fasterxml.jackson.jr.stree.JrsString;
import com.fasterxml.jackson.jr.stree.JrsValue;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
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

    public TypeName toJavaType(String type) {
        switch (type.toLowerCase(Locale.ENGLISH)) {
            case "boolean":
                return TypeName.get(Boolean.class);
            case "string":
                return TypeName.get(String.class);
            case "stringarray":
                return ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(String.class));
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
            case "stringarray":
                methodName = "fromArray";
                param = CodeBlock.of("$L.stream().map($T::fromStr).collect($T.toList())",
                                     param,
                                     rulesRuntimeClassName("Value"),
                                     Collectors.class);
                break;
            default:
                throw new RuntimeException("Don't know how to create a Value instance from type " + type);
        }

        return CodeBlock.builder()
                        .add("$T.$N($L)", rulesRuntimeClassName("Value"), methodName, param)
                        .build();
    }

    public TypeName parameterType(ParameterModel param) {
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

    public boolean useS3Express() {
        return intermediateModel.getCustomizationConfig().getS3ExpressAuthSupport();
    }

    public TypeName resolverReturnType() {
        return ParameterizedTypeName.get(CompletableFuture.class, Endpoint.class);
    }

    public List<String> rulesEngineResourceFiles() {
        URL currentJarUrl = EndpointRulesSpecUtils.class.getProtectionDomain().getCodeSource().getLocation();
        try (JarFile jarFile = new JarFile(currentJarUrl.getFile())) {
            return jarFile.stream()
                          .map(ZipEntry::getName)
                          .filter(e -> e.startsWith("software/amazon/awssdk/codegen/rules/"))
                          .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Map<String, ParameterModel> parameters() {
        return intermediateModel.getEndpointRuleSetModel().getParameters();
    }

    public boolean isDeclaredParam(String paramName) {
        Map<String, ParameterModel> parameters = intermediateModel.getEndpointRuleSetModel().getParameters();
        return parameters.containsKey(paramName);
    }

    /**
     * Creates a data-class level field for the given parameter. For instance
     *
     * <pre>
     *     private final Region region;
     * </pre>
     */
    public FieldSpec parameterClassField(String name, ParameterModel model) {
        return parameterFieldSpecBuilder(name, model)
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.FINAL)
            .build();
    }

    /**
     * Creates a data-class method to access the given parameter. For instance
     *
     * <pre>
     *     public Region region() {…};
     * </pre>
     */
    public List<MethodSpec> parameterClassAccessorMethods(String name, ParameterModel model) {
        String variableName = variableName(name);

        MethodSpec.Builder b = parameterMethodBuilder(name, model);
        b.returns(parameterType(model));
        b.addStatement("return $N", variableName);
        MethodSpec method = b.build();

        // Add a string-based getter for region parameters.
        if (model.getBuiltInEnum() == BuiltInParameter.AWS_REGION) {
            MethodSpec.Builder idB = parameterMethodBuilder(name + "Id", model);
            idB.returns(String.class);
            idB.addStatement("return $1N == null ? null : $1N.id()", variableName);

            MethodSpec idMethod = idB.build();
            return Arrays.asList(method, idMethod);
        }

        return Collections.singletonList(method);
    }


    /**
     * Creates a data-interface method to access the given parameter. For instance
     *
     * <pre>
     *     Region region();
     * </pre>
     */
    public List<MethodSpec> parameterInterfaceAccessorMethods(String name, ParameterModel model) {
        MethodSpec.Builder methodBuilder = parameterMethodBuilder(name, model);
        methodBuilder.returns(parameterType(model));
        methodBuilder.addModifiers(Modifier.ABSTRACT);

        // Add a string-based getter for region parameters.
        if (model.getBuiltInEnum() == BuiltInParameter.AWS_REGION) {
            MethodSpec.Builder idMethodBuilder = parameterMethodBuilder(name + "Id", model);
            idMethodBuilder.returns(String.class);
            idMethodBuilder.addModifiers(Modifier.ABSTRACT);

            return Arrays.asList(methodBuilder.build(), idMethodBuilder.build());
        }

        return Collections.singletonList(methodBuilder.build());
    }

    /**
     * Creates a builder-class level field for the given parameter initialized to its default value when present. For instance
     *
     * <pre>
     *    private Boolean useGlobalEndpoint = false;
     * </pre>
     */
    public FieldSpec parameterBuilderFieldSpec(String name, ParameterModel model) {
        return parameterFieldSpecBuilder(name, model)
            .initializer(parameterDefaultValueCode(model))
            .build();
    }

    /**
     * Creates a builder-interface method to set the given parameter. For instance
     *
     * <pre>
     *    Builder region(Region region);
     * </pre>
     *
     */
    public MethodSpec parameterBuilderSetterMethodDeclaration(ClassName containingClass, String name, ParameterModel model) {
        MethodSpec.Builder b = parameterMethodBuilder(name, model);
        b.addModifiers(Modifier.ABSTRACT);
        b.addParameter(parameterSpec(name, model));
        b.returns(containingClass.nestedClass("Builder"));
        return b.build();
    }

    /**
     * Creates a builder-class method to set the given parameter. For instance
     *
     * <pre>
     *    public Builder region(Region region) {…};
     * </pre>
     */
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

    /**
     * Used internally to create a field for the given parameter. Returns the builder that can be further tailor to be used for
     * data-classes or for builder-classes.
     */
    private FieldSpec.Builder parameterFieldSpecBuilder(String name, ParameterModel model) {
        return FieldSpec.builder(parameterType(model), variableName(name))
                        .addModifiers(Modifier.PRIVATE);
    }

    /**
     * Used internally to create the spec for a parameter to be used in a method for the given param model. For instance, for
     * {@code ParameterModel} for {@code Region} it creates this parameter for the builder setter.
     *
     * <pre>
     *    public Builder region(
     *          Region region // <<--- This
     *          ) {…};
     * </pre>
     */
    private ParameterSpec parameterSpec(String name, ParameterModel model) {
        return ParameterSpec.builder(parameterType(model), variableName(name)).build();
    }

    /**
     * Used internally to create a accessor method for the given parameter model. Returns the builder that can be further
     * tailor to be used for data-classes/interfaces and builder-classes/interfaces.
     */
    private MethodSpec.Builder parameterMethodBuilder(String name, ParameterModel model) {
        MethodSpec.Builder b = MethodSpec.methodBuilder(paramMethodName(name));
        b.addModifiers(Modifier.PUBLIC);
        if (model.getDeprecated() != null) {
            b.addAnnotation(Deprecated.class);
        }
        return b;
    }

    /**
     * Used internally to create the code to initialize the default value modeled for the given parameter. For instance, if the
     * modeled default for region is "us-east-1", it will create
     *
     * <pre>
     *     Region.of("us-east-1")
     * </pre>
     *
     * and if the modeled default value for a boolean parameter useGlobalEndpoint is "false", it will create
     *
     * <pre>
     *     false
     * </pre>
     */
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
            case START_ARRAY:
                handleArrayDefaultValue(b, parameterModel.getType(), (JrsArray) defaultValue);
                break;

            default:
                throw new RuntimeException("Don't know how to set default value for parameter of type "
                                           + defaultValue.asToken());
        }
        return b.build();
    }

    /**
     * Returns the name as a variable name using the intermediate model naming strategy.
     */
    public String variableName(String name) {
        return intermediateModel.getNamingStrategy().getVariableName(name);
    }

    private void handleArrayDefaultValue(CodeBlock.Builder b, String parameterType, JrsArray defaultValue) {
        if ("stringarray".equalsIgnoreCase(parameterType)) {
            Iterator<JrsValue> elementValuesIter = defaultValue.elements();
            b.add("$T.asList(", Arrays.class);
            StringJoiner joinerStr = new StringJoiner(",");
            while (elementValuesIter.hasNext()) {
                joinerStr.add("\"" + elementValuesIter.next().asText() + "\"");
            }
            b.add(joinerStr.toString());
            b.add(")");
        }
    }
}
