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

package software.amazon.awssdk.codegen.poet.client;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.http.auth.scheme.BearerAuthScheme;
import software.amazon.awssdk.identity.spi.TokenIdentity;

public class EnvironmentTokenMetricsInterceptorClass implements ClassSpec {
    protected final PoetExtension poetExtensions;

    public EnvironmentTokenMetricsInterceptorClass(IntermediateModel model) {
        this.poetExtensions = new PoetExtension(model);
    }


    @Override
    public TypeSpec poetSpec() {
        return TypeSpec.classBuilder(className())
                       .addModifiers(Modifier.PUBLIC)
                       .addAnnotation(PoetUtils.generatedAnnotation())
                       .addAnnotation(SdkInternalApi.class)
                       .addSuperinterface(ExecutionInterceptor.class)
                       .addField(String.class, "tokenFromEnv", Modifier.PRIVATE, Modifier.FINAL)
                       .addMethod(constructor())
                       .addMethod(beforeExecutionMethod())
                       .build();
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(String.class, "tokenFromEnv")
                         .addStatement("this.tokenFromEnv = tokenFromEnv")
                         .build();
    }

    @Override
    public ClassName className() {
        return poetExtensions.getEnvironmentTokenMetricsInterceptorClass();
    }

    private MethodSpec beforeExecutionMethod() {
        return MethodSpec
            .methodBuilder("beforeMarshalling")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(Context.BeforeMarshalling.class, "context")
            .addParameter(ExecutionAttributes.class, "executionAttributes")
            .addStatement("$T<?> selectedAuthScheme = executionAttributes.getAttribute($T.SELECTED_AUTH_SCHEME)",
                          SelectedAuthScheme.class, SdkInternalExecutionAttribute.class)
            .beginControlFlow("if (selectedAuthScheme != null && selectedAuthScheme.authSchemeOption().schemeId().equals($T"
                              + ".SCHEME_ID) && selectedAuthScheme.identity().isDone())", BearerAuthScheme.class)
            .beginControlFlow("if (selectedAuthScheme.identity().getNow(null) instanceof $T)", TokenIdentity.class)

            .addStatement("$T configuredToken = ($T) selectedAuthScheme.identity().getNow(null)",
                          TokenIdentity.class, TokenIdentity.class)
            .beginControlFlow("if (configuredToken.token().equals(tokenFromEnv))")
            .addStatement("executionAttributes.getAttribute($T.BUSINESS_METRICS)"
                          + ".addMetric($T.BEARER_SERVICE_ENV_VARS.value())",
                          SdkInternalExecutionAttribute.class, BusinessMetricFeatureId.class)
            .endControlFlow()
            .endControlFlow()
            .endControlFlow()
            .build();
    }
}
