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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.async.AsyncRequestProvider;
import software.amazon.awssdk.async.AsyncResponseHandler;
import software.amazon.awssdk.auth.DefaultCredentialsProvider;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

public class AsyncClientInterface implements ClassSpec {

    public static final TypeVariableName STREAMING_TYPE_VARIABLE = TypeVariableName.get("ReturnT");

    protected final IntermediateModel model;
    protected final ClassName className;
    protected final String clientPackageName;
    private final String modelPackage;

    public AsyncClientInterface(IntermediateModel model) {
        this.modelPackage = model.getMetadata().getFullModelPackageName();
        this.clientPackageName = model.getMetadata().getFullClientPackageName();
        this.model = model;
        this.className = ClassName.get(model.getMetadata().getFullClientPackageName(),
                                       model.getMetadata().getAsyncInterface());
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createInterfaceBuilder(className)
                        .addSuperinterface(AutoCloseable.class)
                        .addJavadoc(getJavadoc())
                        .addMethod(create())
                        .addMethod(builder())
                        .addMethods(operations())
                        .build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    private String getJavadoc() {
        return "Service client for accessing " + model.getMetadata().getServiceAbbreviation() + " asynchronously. This can be "
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
        ClassName builderClass = ClassName.get(clientPackageName, model.getMetadata().getAsyncBuilder());
        ClassName builderInterface = ClassName.get(clientPackageName, model.getMetadata().getAsyncBuilderInterface());
        return MethodSpec.methodBuilder("builder")
                         .returns(builderInterface)
                         .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                         .addJavadoc("Create a builder that can be used to configure and create a {@link $T}.", className)
                         .addStatement("return new $T()", builderClass)
                         .build();
    }

    protected final Iterable<MethodSpec> operations() {
        return model.getOperations().values().stream()
                    .map(this::operationSignatureAndJavaDoc)
                    .map(b -> this.operationBody(b.builder, b.opModel))
                    .map(MethodSpec.Builder::build)
                    .collect(Collectors.toList());
    }

    protected MethodSpec.Builder operationBody(MethodSpec.Builder builder, OperationModel operationModel) {
        return builder.addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                      .addStatement("throw new $T()", UnsupportedOperationException.class);
    }

    private BuilderModelBag operationSignatureAndJavaDoc(OperationModel opModel) {
        ClassName responsePojoType = ClassName.get(modelPackage, opModel.getReturnType().getReturnType());
        ClassName requestType = ClassName.get(modelPackage, opModel.getInput().getVariableType());

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(opModel.getMethodName())
                                                     .returns(getAsyncReturnType(opModel, responsePojoType))
                                                     .addParameter(requestType, opModel.getInput().getVariableName())
                                                     .addJavadoc(opModel.getAsyncDocumentation(model, opModel));

        if (opModel.hasStreamingInput()) {
            methodBuilder.addParameter(ClassName.get(AsyncRequestProvider.class), "requestProvider");
        }
        if (opModel.hasStreamingOutput()) {
            methodBuilder.addTypeVariable(STREAMING_TYPE_VARIABLE);
            final ParameterizedTypeName asyncResponseHandlerType = ParameterizedTypeName
                    .get(ClassName.get(AsyncResponseHandler.class), responsePojoType, STREAMING_TYPE_VARIABLE);
            methodBuilder.addParameter(asyncResponseHandlerType, "asyncResponseHandler");
        }
        return new BuilderModelBag(methodBuilder, opModel);
    }

    /**
     * Get the return {@link TypeName} of an async method. Depends on whether it's streaming or not.
     *
     * @param opModel          Operation to get return type for.
     * @param responsePojoType Type of Response POJO.
     * @return Return type of the operation method.
     */
    private TypeName getAsyncReturnType(OperationModel opModel, ClassName responsePojoType) {
        if (opModel.hasStreamingOutput()) {
            return ParameterizedTypeName.get(ClassName.get(CompletableFuture.class), STREAMING_TYPE_VARIABLE);
        } else {
            return ParameterizedTypeName.get(ClassName.get(CompletableFuture.class), responsePojoType);
        }
    }

    private static class BuilderModelBag {
        private final MethodSpec.Builder builder;
        private final OperationModel opModel;

        private BuilderModelBag(MethodSpec.Builder builder, OperationModel opModel) {
            this.builder = builder;
            this.opModel = opModel;
        }
    }
}
