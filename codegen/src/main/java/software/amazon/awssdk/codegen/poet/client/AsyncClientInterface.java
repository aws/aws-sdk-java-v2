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

import static java.util.stream.Collectors.toList;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.docs.ClientType;
import software.amazon.awssdk.codegen.docs.SimpleMethodOverload;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.async.AsyncRequestProvider;
import software.amazon.awssdk.core.async.AsyncResponseHandler;
import software.amazon.awssdk.core.auth.DefaultCredentialsProvider;
import software.amazon.awssdk.core.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.utils.SdkAutoCloseable;

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
                        .addSuperinterface(SdkAutoCloseable.class)
                        .addJavadoc(getJavadoc())
                        .addMethod(create())
                        .addMethod(builder())
                        .addMethods(operationsAndSimpleMethods())
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
                         .addJavadoc("Create a {@link $T} with the region loaded from the {@link $T} and credentials loaded "
                                     + "from the {@link $T}.",
                                     className,
                                     DefaultAwsRegionProviderChain.class,
                                     DefaultCredentialsProvider.class)
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

    /**
     * @return List generated of traditional (request/response) methods for all operations.
     */
    protected final List<MethodSpec> operations() {
        return model.getOperations().values().stream()
                    .map(this::traditionalMethod)
                    .map(MethodSpec.Builder::build)
                    .collect(toList());
    }

    /**
     * @return Traditional request/response methods plus any additional simple method overloads (for no-args and streaming for
     * example).
     */
    private Iterable<MethodSpec> operationsAndSimpleMethods() {
        List<MethodSpec> methods = operations();
        methods.addAll(model.getOperations().values().stream()
                            .map(this::addMethodOverloads)
                            .flatMap(List::stream)
                            .map(MethodSpec.Builder::build)
                            .collect(toList()));
        return methods.stream().sorted(Comparator.comparing(m -> m.name)).collect(toList());
    }

    /**
     * @param opModel Operation to generate simple methods for.
     * @return All simple method overloads for a given operation.
     */
    private List<MethodSpec.Builder> addMethodOverloads(OperationModel opModel) {
        List<MethodSpec.Builder> methodOverloads = new ArrayList<>();
        if (opModel.getInputShape().isSimpleMethod()) {
            methodOverloads.add(noArgSimpleMethod(opModel));
        }
        if (opModel.hasStreamingInput()) {
            methodOverloads.add(streamingInputFileSimpleMethod(opModel));
        }
        if (opModel.hasStreamingOutput()) {
            methodOverloads.add(streamingOutputFileSimpleMethod(opModel));
        }
        if (!opModel.isStreaming()) {
            methodOverloads.add(builderConsumerMethod(opModel));
        }
        return methodOverloads;
    }

    /**
     * Add the implementation body. The interface implements all methods by throwing an {@link UnsupportedOperationException}
     * except for simple method overloads which just delegate to the traditional request/response method. This is overridden
     * in {@link AsyncClientClass} to add an actual implementation.
     *
     * @param builder        Current {@link com.squareup.javapoet.MethodSpec.Builder} to add implementation to.
     * @param operationModel Operation to generate method body for.
     * @return Builder with method body added.
     */
    protected MethodSpec.Builder operationBody(MethodSpec.Builder builder, OperationModel operationModel) {
        return builder.addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                      .addStatement("throw new $T()", UnsupportedOperationException.class);
    }

    /**
     * Generates the traditional method for an operation (i.e. one that takes a request and returns a response).
     */
    private MethodSpec.Builder traditionalMethod(OperationModel opModel) {
        ClassName responsePojoType = getPojoResponseType(opModel);
        ClassName requestType = ClassName.get(modelPackage, opModel.getInput().getVariableType());

        MethodSpec.Builder builder = methodSignatureWithReturnType(opModel)
                .addParameter(requestType, opModel.getInput().getVariableName())
                .addJavadoc(opModel.getDocs(model, ClientType.ASYNC));

        if (opModel.hasStreamingInput()) {
            builder.addParameter(ClassName.get(AsyncRequestProvider.class), "requestProvider");
        }
        if (opModel.hasStreamingOutput()) {
            builder.addTypeVariable(STREAMING_TYPE_VARIABLE);
            final ParameterizedTypeName asyncResponseHandlerType = ParameterizedTypeName
                    .get(ClassName.get(AsyncResponseHandler.class), responsePojoType, STREAMING_TYPE_VARIABLE);
            builder.addParameter(asyncResponseHandlerType, "asyncResponseHandler");
        }

        return operationBody(builder, opModel);
    }

    /**
     * Generate a simple method that takes no arguments for operations with no required parameters.
     */
    private MethodSpec.Builder noArgSimpleMethod(OperationModel opModel) {
        return interfaceMethodSignature(opModel)
                .addJavadoc(opModel.getDocs(model, ClientType.ASYNC, SimpleMethodOverload.NO_ARG))
                .addStatement("return $N($N.builder().build())",
                              opModel.getMethodName(),
                              opModel.getInput().getVariableType());
    }


    /**
     * Creates a method that thats a Consumer of Request.Builder
     */
    private MethodSpec.Builder builderConsumerMethod(OperationModel opModel) {
        ClassName requestType = ClassName.get(model.getMetadata().getFullModelPackageName(),
                                              opModel.getInput().getVariableType());
        ClassName builder = requestType.nestedClass("Builder");
        TypeName consumer = ParameterizedTypeName.get(ClassName.get(Consumer.class), builder);

        return interfaceMethodSignature(opModel)
            .addParameter(consumer, opModel.getInput().getVariableName())
            .addJavadoc(opModel.getDocs(model, ClientType.ASYNC, SimpleMethodOverload.CONSUMER_BUILDER))
            .addStatement("return $N($T.builder().apply($N).build())",
                          opModel.getMethodName(),
                          requestType,
                          opModel.getInput().getVariableName());
    }

    /**
     * Generate a simple method for operations with a streaming input member that takes a {@link Path} containing the data
     * to upload.
     */
    private MethodSpec.Builder streamingInputFileSimpleMethod(OperationModel opModel) {
        ClassName requestType = ClassName.get(modelPackage, opModel.getInput().getVariableType());
        return interfaceMethodSignature(opModel)
                .addJavadoc(opModel.getDocs(model, ClientType.ASYNC, SimpleMethodOverload.FILE))
                .addParameter(requestType, opModel.getInput().getVariableName())
                .addParameter(ClassName.get(Path.class), "path")
                .addStatement("return $L($L, $T.fromFile(path))", opModel.getMethodName(),
                              opModel.getInput().getVariableName(),
                              ClassName.get(AsyncRequestProvider.class));
    }

    /**
     * Generate a simple method for operations with a streaming output member that takes a {@link Path} where data
     * will be downloaded to.
     */
    private MethodSpec.Builder streamingOutputFileSimpleMethod(OperationModel opModel) {
        ClassName requestType = ClassName.get(modelPackage, opModel.getInput().getVariableType());
        return interfaceMethodSignature(opModel)
                .returns(
                        completableFutureType(getPojoResponseType(opModel)))
                .addJavadoc(opModel.getDocs(model, ClientType.ASYNC, SimpleMethodOverload.FILE))
                .addParameter(requestType, opModel.getInput().getVariableName())
                .addParameter(ClassName.get(Path.class), "path")
                .addStatement("return $L($L, $T.toFile(path))", opModel.getMethodName(),
                              opModel.getInput().getVariableName(),
                              ClassName.get(AsyncResponseHandler.class));
    }

    /**
     * Factory method for creating a {@link com.squareup.javapoet.MethodSpec.Builder} with correct return type.
     *
     * @return MethodSpec with only return type set.
     */
    private MethodSpec.Builder methodSignatureWithReturnType(OperationModel opModel) {
        ClassName responsePojoType = getPojoResponseType(opModel);
        return MethodSpec.methodBuilder(opModel.getMethodName())
                         .returns(getAsyncReturnType(opModel, responsePojoType));
    }

    /**
     * Factory method for creating a {@link com.squareup.javapoet.MethodSpec.Builder} with
     * correct return type and public/default modifiers for use in interfaces.
     *
     * @return MethodSpec with public/default modifiers for interface file.
     */
    private MethodSpec.Builder interfaceMethodSignature(OperationModel opModel) {
        return methodSignatureWithReturnType(opModel)
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT);
    }

    /**
     * @return ClassName for POJO response class.
     */
    private ClassName getPojoResponseType(OperationModel opModel) {
        return ClassName.get(modelPackage, opModel.getReturnType().getReturnType());
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
            return completableFutureType(STREAMING_TYPE_VARIABLE);
        } else {
            return completableFutureType(responsePojoType);
        }
    }

    /**
     * Returns a {@link ParameterizedTypeName} of {@link CompletableFuture} with the given typeName as the type parameter.
     */
    private ParameterizedTypeName completableFutureType(TypeName typeName) {
        return ParameterizedTypeName.get(ClassName.get(CompletableFuture.class), typeName);
    }
}
