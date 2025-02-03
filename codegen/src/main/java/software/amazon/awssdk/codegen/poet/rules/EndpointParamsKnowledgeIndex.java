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

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.endpoints.AccountIdEndpointMode;
import software.amazon.awssdk.awscore.endpoints.AccountIdEndpointModeResolver;
import software.amazon.awssdk.awscore.internal.useragent.BusinessMetricsUtils;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.internal.LocalParameter;
import software.amazon.awssdk.codegen.model.rules.endpoints.BuiltInParameter;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

/**
 * Knowledge index to get access to endpoint parameters known to the client builder classes.
 */
public final class EndpointParamsKnowledgeIndex {
    private static final Map<BuiltInParameter, LocalParameter> BUILT_IN_PARAMS_FOR_CLIENT_BUILDER =
        new EnumMap<>(BuiltInParameter.class);
    private final IntermediateModel intermediateModel;
    private Map<BuiltInParameter, LocalParameter> parametersToGenerate = new EnumMap<>(BuiltInParameter.class);

    static {
        BUILT_IN_PARAMS_FOR_CLIENT_BUILDER.put(
            BuiltInParameter.AWS_AUTH_ACCOUNT_ID_ENDPOINT_MODE,
            new LocalParameter("accountIdEndpointMode",
                               AccountIdEndpointMode.class,
                               CodeBlock.of("Sets the behavior when account ID based endpoints are created. "
                                             + "See {@link $T} for values", AccountIdEndpointMode.class)));
    }

    private EndpointParamsKnowledgeIndex(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.parametersToGenerate = builtInsForClientBuilder(intermediateModel.getEndpointRuleSetModel().getParameters());
    }

    /**
     * Creates a new {@link EndpointParamsKnowledgeIndex} using the given {@code intermediateModel}..
     */
    public static EndpointParamsKnowledgeIndex of(IntermediateModel intermediateModel) {
        return new EndpointParamsKnowledgeIndex(intermediateModel);
    }

    public boolean hasAccountIdEndpointModeBuiltIn() {
        return parametersToGenerate.containsKey(BuiltInParameter.AWS_AUTH_ACCOUNT_ID_ENDPOINT_MODE);
    }

    public Optional<MethodSpec> accountIdEndpointModeClassMethodSpec() {
        if (hasAccountIdEndpointModeBuiltIn()) {
            return Optional.of(clientClassBuilderParamSetter(accountIdEndpointModeBuiltInParam()));
        }
        return Optional.empty();
    }

    public Optional<MethodSpec> accountIdEndpointModeInterfaceMethodSpec() {
        if (hasAccountIdEndpointModeBuiltIn()) {
            return Optional.of(clientInterfaceBuilderParamSetter(accountIdEndpointModeBuiltInParam()));
        }
        return Optional.empty();
    }

    private LocalParameter accountIdEndpointModeBuiltInParam() {
        return parametersToGenerate.get(BuiltInParameter.AWS_AUTH_ACCOUNT_ID_ENDPOINT_MODE);
    }

    private MethodSpec clientClassBuilderParamSetter(LocalParameter param) {
        String setterName = Utils.unCapitalize(CodegenNamingUtils.pascalCase(param.name()));
        String keyName = intermediateModel.getNamingStrategy().getEnumValueName(param.name());
        TypeName type = TypeName.get(param.type());

        return MethodSpec.methodBuilder(setterName)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(TypeVariableName.get("B"))
                         .addParameter(type, setterName)
                         .addStatement("clientConfiguration.option($T.$L, $L)",
                                       AwsClientOption.class, keyName, setterName)
                         .addStatement("return thisBuilder()")
                         .build();
    }

    private MethodSpec clientInterfaceBuilderParamSetter(LocalParameter param) {
        String setterName = Utils.unCapitalize(CodegenNamingUtils.pascalCase(param.name()));
        TypeName type = TypeName.get(param.type());

        MethodSpec.Builder b = MethodSpec.methodBuilder(setterName)
                                         .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                         .addParameter(type, setterName)
                                         .addJavadoc(param.documentation())
                                         .returns(TypeVariableName.get("B"));

        return b.build();
    }

    public Optional<MethodSpec> resolveAccountIdEndpointModeMethod() {
        if (!hasAccountIdEndpointModeBuiltIn()) {
            return Optional.empty();
        }

        String name = "accountIdEndpointMode";
        String keyName = intermediateModel.getNamingStrategy().getEnumValueName(name);
        TypeName typeName = TypeName.get(AccountIdEndpointMode.class);

        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveAccountIdEndpointMode")
                                               .addModifiers(PRIVATE)
                                               .addParameter(SdkClientConfiguration.class, "config")
                                               .returns(typeName);

        builder.addStatement("$T configuredMode = config.option($T.$L)", typeName, AwsClientOption.class, keyName);

        builder.beginControlFlow("if (configuredMode == null)");
        builder.addCode("configuredMode = $T.create()", AccountIdEndpointModeResolver.class);
        builder.addCode(".profileFile(config.option($T.PROFILE_FILE_SUPPLIER))", SdkClientOption.class);
        builder.addCode(".profileName(config.option($T.PROFILE_NAME))", SdkClientOption.class);
        builder.addCode(".defaultMode($T.PREFERRED)", typeName);
        builder.addStatement(".resolve()");
        builder.endControlFlow();

        builder.addStatement("return configuredMode");
        return Optional.of(builder.build());
    }

    private Map<BuiltInParameter, LocalParameter> builtInsForClientBuilder(Map<String, ParameterModel> serviceEndpointParams) {
        Map<BuiltInParameter, LocalParameter> actualParams = new EnumMap<>(BuiltInParameter.class);
        serviceEndpointParams.forEach((k, v) -> {
            BuiltInParameter builtInEnum = v.getBuiltInEnum();
            if (builtInEnum != null && BUILT_IN_PARAMS_FOR_CLIENT_BUILDER.containsKey(builtInEnum)) {
                actualParams.put(builtInEnum, BUILT_IN_PARAMS_FOR_CLIENT_BUILDER.get(builtInEnum));
            }
        });
        return actualParams;
    }

    public void addAccountIdMethodsIfPresent(TypeSpec.Builder b) {
        if (!hasAccountIdEndpointModeBuiltIn()) {
            return;
        }

        b.addMethod(resolveAndRecordAccountIdFromIdentityMethod());
        b.addMethod(accountIdFromIdentityMethod());
        b.addMethod(recordAccountIdEndpointModeMethod());
    }

    public MethodSpec recordAccountIdEndpointModeMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("recordAccountIdEndpointMode")
                                               .addModifiers(PRIVATE, STATIC)
                                               .addParameter(ExecutionAttributes.class, "executionAttributes")
                                               .returns(String.class);
        builder.addStatement("$T mode = executionAttributes.getAttribute($T.AWS_AUTH_ACCOUNT_ID_ENDPOINT_MODE)",
                             AccountIdEndpointMode.class, AwsExecutionAttribute.class);

        builder.addStatement("$T.resolveAccountIdEndpointModeMetric(mode)"
                             + ".ifPresent(m -> executionAttributes.getAttribute($T.BUSINESS_METRICS).addMetric(m))",
                             BusinessMetricsUtils.class, SdkInternalExecutionAttribute.class);

        builder.addStatement("return mode.name().toLowerCase()");

        return builder.build();
    }

    public MethodSpec resolveAndRecordAccountIdFromIdentityMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveAndRecordAccountIdFromIdentity")
                                               .addModifiers(PRIVATE, STATIC)
                                               .addParameter(ExecutionAttributes.class, "executionAttributes")
                                               .returns(String.class);
        builder.addStatement("$T accountId = accountIdFromIdentity(executionAttributes.getAttribute($T.SELECTED_AUTH_SCHEME))",
                             String.class, SdkInternalExecutionAttribute.class);

        builder.addStatement("executionAttributes.getAttribute($T.BUSINESS_METRICS).addMetric($T.RESOLVED_ACCOUNT_ID.value())",
                             SdkInternalExecutionAttribute.class, BusinessMetricFeatureId.class);

        builder.addStatement("return accountId");

        return builder.build();
    }


    public MethodSpec accountIdFromIdentityMethod() {
        ParameterizedTypeName paramType = ParameterizedTypeName.get(ClassName.get(SelectedAuthScheme.class),
                                                                    TypeVariableName.get("T"));

        MethodSpec.Builder builder = MethodSpec.methodBuilder("accountIdFromIdentity")
                                               .addModifiers(PRIVATE, STATIC)
                                               .addTypeVariable(TypeVariableName.get("T", Identity.class))
                                               .addParameter(paramType, "selectedAuthScheme")
                                               .returns(String.class);

        builder.addStatement("$T identity = $T.joinLikeSync(selectedAuthScheme.identity())", TypeVariableName.get("T"),
                             CompletableFutureUtils.class);
        builder.addStatement("$T accountId = null", String.class);
        builder.beginControlFlow("if (identity instanceof $T)", AwsCredentialsIdentity.class);
        builder.addStatement("accountId = (($T) identity).accountId().orElse(null)", AwsCredentialsIdentity.class);
        builder.endControlFlow();
        builder.addStatement("return accountId");
        return builder.build();
    }
}
