/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.auth.Aws4Signer;
import software.amazon.awssdk.core.auth.QueryStringSigner;
import software.amazon.awssdk.core.auth.StaticSignerProvider;
import software.amazon.awssdk.core.client.builder.DefaultClientBuilder;
import software.amazon.awssdk.core.config.defaults.ClientConfigurationDefaults;
import software.amazon.awssdk.core.config.defaults.ServiceBuilderConfigurationDefaults;
import software.amazon.awssdk.core.runtime.auth.SignerProvider;
import software.amazon.awssdk.utils.AttributeMap;

public class BaseClientBuilderClass implements ClassSpec {
    private final IntermediateModel model;
    private final ClassName builderInterfaceName;
    private final ClassName builderClassName;
    private final String basePackage;

    public BaseClientBuilderClass(IntermediateModel model) {
        this.model = model;
        this.basePackage = model.getMetadata().getFullClientPackageName();
        this.builderInterfaceName = ClassName.get(basePackage, model.getMetadata().getBaseBuilderInterface());
        this.builderClassName = ClassName.get(basePackage, model.getMetadata().getBaseBuilder());
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder =
                PoetUtils.createClassBuilder(builderClassName)
                         .addModifiers(Modifier.ABSTRACT)
                         .addAnnotation(SdkInternalApi.class)
                         .addTypeVariable(PoetUtils.createBoundedTypeVariableName("B", builderInterfaceName, "B", "C"))
                         .addTypeVariable(TypeVariableName.get("C"))
                         .superclass(PoetUtils.createParameterizedTypeName(DefaultClientBuilder.class, "B", "C"))
                         .addJavadoc("Internal base class for {@link $T} and {@link $T}.",
                                     ClassName.get(basePackage, model.getMetadata().getSyncBuilder()),
                                     ClassName.get(basePackage, model.getMetadata().getAsyncBuilder()));

        if (model.getCustomizationConfig().getServiceSpecificClientConfigClass() != null) {
            ClassName advancedConfiguration = ClassName.get(basePackage,
                                                            model.getCustomizationConfig().getServiceSpecificClientConfigClass());
            builder.addField(FieldSpec.builder(advancedConfiguration, "advancedConfiguration")
                                      .addModifiers(Modifier.PRIVATE)
                                      .build());
        }

        builder.addMethod(serviceEndpointPrefixMethod());
        builder.addMethod(serviceDefaultsMethod());
        builder.addMethod(defaultSignerProviderMethod());

        if (model.getCustomizationConfig().getServiceSpecificClientConfigClass() != null) {
            builder.addMethod(setAdvancedConfigurationMethod())
                   .addMethod(getAdvancedConfigurationMethod())
                   .addMethod(beanStyleSetAdvancedConfigurationMethod());
        }

        if (model.getCustomizationConfig().getServiceSpecificHttpConfig() != null) {
            builder.addMethod(serviceSpecificHttpConfigMethod());
        }

        return builder.build();
    }

    private MethodSpec serviceEndpointPrefixMethod() {
        return MethodSpec.methodBuilder("serviceEndpointPrefix")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                         .returns(String.class)
                         .addCode("return $S;", model.getMetadata().getEndpointPrefix())
                         .build();
    }

    private MethodSpec serviceDefaultsMethod() {
        String requestHandlerDirectory = Utils.packageToDirectory(model.getMetadata().getFullClientPackageName());
        String requestHandlerPath = String.format("%s/execution.interceptors", requestHandlerDirectory);

        boolean crc32FromCompressedDataEnabled = model.getCustomizationConfig().isCalculateCrc32FromCompressedData();

        return MethodSpec.methodBuilder("serviceDefaults")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                         .returns(ClientConfigurationDefaults.class)
                         .addCode("return $T.builder()\n", ServiceBuilderConfigurationDefaults.class)
                         .addCode("         .defaultSignerProvider(this::defaultSignerProvider)\n")
                         .addCode("         .addRequestHandlerPath($S)\n", requestHandlerPath)
                         .addCode("         .crc32FromCompressedDataEnabled($L)\n", crc32FromCompressedDataEnabled)
                         .addCode("         .build();\n")
                         .build();
    }

    private MethodSpec setAdvancedConfigurationMethod() {
        ClassName advancedConfiguration = ClassName.get(basePackage,
                                                        model.getCustomizationConfig().getServiceSpecificClientConfigClass());
        return MethodSpec.methodBuilder("advancedConfiguration")
                         .addModifiers(Modifier.PUBLIC)
                         .returns(TypeVariableName.get("B"))
                         .addParameter(advancedConfiguration, "advancedConfiguration")
                         .addStatement("this.advancedConfiguration = advancedConfiguration")
                         .addStatement("return thisBuilder()")
                         .build();
    }

    private MethodSpec getAdvancedConfigurationMethod() {
        ClassName advancedConfiguration = ClassName.get(basePackage,
                                                        model.getCustomizationConfig().getServiceSpecificClientConfigClass());
        return MethodSpec.methodBuilder("advancedConfiguration")
                         .addModifiers(Modifier.PROTECTED)
                         .returns(advancedConfiguration)
                         .addStatement("return advancedConfiguration")
                         .build();
    }

    private MethodSpec beanStyleSetAdvancedConfigurationMethod() {
        ClassName advancedConfiguration = ClassName.get(basePackage,
                                                        model.getCustomizationConfig().getServiceSpecificClientConfigClass());
        return MethodSpec.methodBuilder("setAdvancedConfiguration")
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(advancedConfiguration, "advancedConfiguration")
                         .addStatement("advancedConfiguration(advancedConfiguration)")
                         .build();
    }

    private MethodSpec serviceSpecificHttpConfigMethod() {
        return MethodSpec.methodBuilder("serviceSpecificHttpConfig")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                         .returns(AttributeMap.class)
                         .addCode("return $L;", model.getCustomizationConfig().getServiceSpecificHttpConfig())
                         .build();
    }

    private MethodSpec defaultSignerProviderMethod() {
        return MethodSpec.methodBuilder("defaultSignerProvider")
                         .returns(SignerProvider.class)
                         .addModifiers(Modifier.PRIVATE)
                         .addCode(signerDefinitionMethodBody())
                         .build();
    }

    private CodeBlock signerDefinitionMethodBody() {
        AuthType authType = model.getMetadata().getAuthType();
        switch (authType) {
            case V4:
                return v4SignerDefinitionMethodBody();
            case V2:
                return v2SignerDefinitionMethodBody();
            case S3:
                return s3SignerDefinitionMethodBody();
            default:
                throw new UnsupportedOperationException("Unsupported signer type: " + authType);
        }
    }

    private CodeBlock v4SignerDefinitionMethodBody() {
        return CodeBlock.of("$T signer = new $T();\n" +
                            "signer.setServiceName($S);\n" +
                            "signer.setRegionName(signingRegion().value());\n" +
                            "return $T.create(signer);\n",
                            Aws4Signer.class,
                            Aws4Signer.class,
                            model.getMetadata().getSigningName(),
                            StaticSignerProvider.class);
    }

    private CodeBlock v2SignerDefinitionMethodBody() {
        return CodeBlock.of("return $T.create(new $T());\n",
                            StaticSignerProvider.class,
                            QueryStringSigner.class);
    }

    private CodeBlock s3SignerDefinitionMethodBody() {
        return CodeBlock.of("$T signer = new $T();\n" +
                            "signer.setServiceName(\"$L\");\n" +
                            "signer.setRegionName(signingRegion().value());\n" +
                            "return $T.create(signer);\n",
                            ClassName.get("software.amazon.awssdk.services.s3", "AwsS3V4Signer"),
                            ClassName.get("software.amazon.awssdk.services.s3", "AwsS3V4Signer"),
                            model.getMetadata().getSigningName(),
                            StaticSignerProvider.class);
    }

    @Override
    public ClassName className() {
        return builderClassName;
    }
}
