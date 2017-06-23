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

package software.amazon.awssdk.codegen.poet.client;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static software.amazon.awssdk.codegen.poet.client.AsyncClientInterface.STREAMING_TYPE_VARIABLE;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.auth.DefaultCredentialsProvider;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.sync.RequestBody;
import software.amazon.awssdk.sync.StreamingResponseHandler;

public final class SyncClientInterface implements ClassSpec {

    private final IntermediateModel model;
    private final ClassName className;
    private final String clientPackageName;

    public SyncClientInterface(IntermediateModel model) {
        this.model = model;
        this.clientPackageName = model.getMetadata().getFullClientPackageName();
        this.className = ClassName.get(clientPackageName, model.getMetadata().getSyncInterface());
    }

    @Override
    public TypeSpec poetSpec() {
        Builder classBuilder = PoetUtils.createInterfaceBuilder(className)
                                        .addSuperinterface(AutoCloseable.class)
                                        .addJavadoc(getJavadoc())
                                        .addField(FieldSpec.builder(String.class, "SERVICE_NAME")
                                                           .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                                           .initializer("$S", model.getMetadata().getSigningName())
                                                           .build())
                                        .addMethod(create())
                                        .addMethod(builder())
                                        .addMethods(operations())
                                        .addMethod(serviceMetadata());

        if (model.getHasWaiters()) {
            classBuilder.addMethod(waiters());
        }
        if (model.getCustomizationConfig().getPresignersFqcn() != null) {
            classBuilder.addMethod(presigners());
        }

        return classBuilder.build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    private String getJavadoc() {
        return "Service client for accessing " + model.getMetadata().getServiceAbbreviation() + ". This can be "
               + "created using the static {@link #builder()} method.\n\n" + model.getMetadata().getDocumentation();
    }

    private MethodSpec create() {
        return MethodSpec.methodBuilder("create")
                 .returns(className)
                 .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                 .addJavadoc("Create a {@link $T} with the region loaded from the {@link $T} and credentials loaded from the "
                             + "{@link $T}.", className, DefaultAwsRegionProviderChain.class, DefaultCredentialsProvider.class)
                 .addStatement("return builder().build()")
                 .build();
    }

    private MethodSpec builder() {
        ClassName builderClass = ClassName.get(clientPackageName, model.getMetadata().getSyncBuilder());
        ClassName builderInterface = ClassName.get(clientPackageName, model.getMetadata().getSyncBuilderInterface());
        return MethodSpec.methodBuilder("builder")
                         .returns(builderInterface)
                         .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                         .addJavadoc("Create a builder that can be used to configure and create a {@link $T}.", className)
                         .addStatement("return new $T()", builderClass)
                         .build();
    }

    private Iterable<MethodSpec> operations() {
        return model.getOperations().values().stream().map(this::operationMethodSpec).collect(toList());
    }

    private MethodSpec serviceMetadata() {
        return MethodSpec.methodBuilder("serviceMetadata")
                         .returns(ServiceMetadata.class)
                         .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                         .addStatement("return $T.of($S)", ServiceMetadata.class, model.getMetadata().getEndpointPrefix())
                         .build();
    }

    private MethodSpec operationMethodSpec(OperationModel opModel) {
        return operationMethodSignature(model, opModel)
                .addModifiers(Modifier.DEFAULT)
                .addStatement("throw new $T()", UnsupportedOperationException.class)
                .build();
    }

    // TODO This is inconsistent with how async client reuses method signature
    public static MethodSpec.Builder operationMethodSignature(IntermediateModel model, OperationModel opModel) {
        TypeName returnType = opModel.hasStreamingOutput() ? STREAMING_TYPE_VARIABLE :
                ClassName.get(model.getMetadata().getFullModelPackageName(), opModel.getReturnType().getReturnType());
        ClassName requestType = ClassName.get(model.getMetadata().getFullModelPackageName(),
                                              opModel.getInput().getVariableType());

        final MethodSpec.Builder method = MethodSpec.methodBuilder(opModel.getMethodName())
                                                    .returns(returnType)
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .addParameter(requestType, opModel.getInput().getVariableName())
                                                    .addJavadoc(opModel.getSyncDocumentation(model, opModel))
                                                    .addExceptions(getExceptionClasses(model, opModel));

        if (opModel.hasStreamingInput()) {
            method.addParameter(ClassName.get(RequestBody.class), "requestBody");
        }
        if (opModel.hasStreamingOutput()) {
            method.addTypeVariable(STREAMING_TYPE_VARIABLE);
            method.addParameter(ClassName.get(StreamingResponseHandler.class), "streamingHandler");
        }
        return method;
    }

    private static List<ClassName> getExceptionClasses(IntermediateModel model, OperationModel opModel) {
        List<ClassName> exceptions = opModel.getExceptions().stream()
                                            .map(e -> ClassName.get(model.getMetadata().getFullModelPackageName(),
                                                                    e.getExceptionName()))
                                            .collect(toCollection(ArrayList::new));
        Collections.addAll(exceptions, ClassName.get(SdkBaseException.class),
                           ClassName.get(SdkClientException.class),
                           ClassName.get(model.getMetadata().getFullModelPackageName(),
                                         model.getSdkModeledExceptionBaseClassName()));
        return exceptions;
    }

    private MethodSpec waiters() {
        return MethodSpec.methodBuilder("waiters")
                         .returns(ClassName.get(model.getMetadata().getFullWaitersPackageName(),
                                                model.getMetadata().getSyncInterface() + "Waiters"))
                         .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                         .build();
    }

    private MethodSpec presigners() {
        ClassName presignerClassName = PoetUtils.classNameFromFqcn(model.getCustomizationConfig().getPresignersFqcn());
        return MethodSpec.methodBuilder("presigners")
                         .returns(presignerClassName)
                         .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                         .build();
    }
}
