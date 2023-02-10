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

import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static software.amazon.awssdk.codegen.internal.Constant.ASYNC_STREAMING_INPUT_PARAM;
import static software.amazon.awssdk.codegen.internal.Constant.EVENT_PUBLISHER_PARAM_NAME;
import static software.amazon.awssdk.codegen.internal.Constant.EVENT_RESPONSE_HANDLER_PARAM_NAME;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
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
import java.util.stream.Stream;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.codegen.docs.ClientType;
import software.amazon.awssdk.codegen.docs.DocConfiguration;
import software.amazon.awssdk.codegen.docs.SimpleMethodOverload;
import software.amazon.awssdk.codegen.docs.WaiterDocs;
import software.amazon.awssdk.codegen.model.config.customization.AdditionalBuilderMethod;
import software.amazon.awssdk.codegen.model.config.customization.UtilitiesMethod;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.eventstream.EventStreamUtils;
import software.amazon.awssdk.codegen.poet.model.DeprecationUtils;
import software.amazon.awssdk.codegen.utils.PaginatorUtils;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.ServiceMetadataProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.utils.Validate;

public class AsyncClientInterface implements ClassSpec {

    public static final TypeVariableName STREAMING_TYPE_VARIABLE = TypeVariableName.get("ReturnT");

    protected final IntermediateModel model;
    protected final ClassName className;
    protected final String clientPackageName;
    private final String modelPackage;
    private final PoetExtension poetExtensions;

    public AsyncClientInterface(IntermediateModel model) {
        this.modelPackage = model.getMetadata().getFullModelPackageName();
        this.clientPackageName = model.getMetadata().getFullClientPackageName();
        this.model = model;
        this.className = ClassName.get(model.getMetadata().getFullClientPackageName(),
                                       model.getMetadata().getAsyncInterface());
        this.poetExtensions = new PoetExtension(model);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder result = createTypeSpec();

        addInterfaceClass(result);
        addAnnotations(result);
        addModifiers(result);
        addFields(result);
        if (model.getCustomizationConfig().getUtilitiesMethod() != null) {
            result.addMethod(utilitiesMethod());
        }
        result.addMethods(operations());
        if (model.hasWaiters()) {
            addWaiterMethod(result);
        }
        addAdditionalMethods(result);
        addCloseMethod(result);
        return result.build();
    }

    protected TypeSpec.Builder createTypeSpec() {
        return PoetUtils.createInterfaceBuilder(className);
    }

    protected void addInterfaceClass(TypeSpec.Builder type) {
        type.addSuperinterface(SdkClient.class);
    }

    protected void addAnnotations(TypeSpec.Builder type) {
        type.addAnnotation(SdkPublicApi.class)
            .addAnnotation(ThreadSafe.class);
    }

    protected void addModifiers(TypeSpec.Builder type) {
    }

    protected void addCloseMethod(TypeSpec.Builder type) {
    }

    protected void addFields(TypeSpec.Builder type) {
        type.addField(FieldSpec.builder(String.class, "SERVICE_NAME")
                               .addModifiers(PUBLIC, STATIC, FINAL)
                               .initializer("$S", model.getMetadata().getSigningName())
                               .build())
            .addField(FieldSpec.builder(String.class, "SERVICE_METADATA_ID")
                               .addModifiers(PUBLIC, STATIC, FINAL)
                               .initializer("$S", model.getMetadata().getEndpointPrefix())
                               .addJavadoc("Value for looking up the service's metadata from the {@link $T}.",
                                           ServiceMetadataProvider.class)
                               .build());
    }

    protected void addAdditionalMethods(TypeSpec.Builder type) {

        if (!model.getCustomizationConfig().isExcludeClientCreateMethod()) {
            type.addMethod(create());
        }

        type.addMethod(builder());

        List<AdditionalBuilderMethod> additionaBuilders = model.getCustomizationConfig().getAdditionalBuilderMethods();
        if (additionaBuilders != null && !additionaBuilders.isEmpty()) {
            additionaBuilders.stream()
                             .filter(builder -> software.amazon.awssdk.core.ClientType.ASYNC.equals(builder.getClientTypeEnum()))
                             .forEach(builders -> type.addMethod(additionalBuilders(builders)));
        }

        PoetUtils.addJavadoc(type::addJavadoc, getJavadoc());
    }

    protected void addWaiterMethod(TypeSpec.Builder type) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("waiter")
                                               .addModifiers(PUBLIC)
                                               .returns(poetExtensions.getAsyncWaiterInterface())
                                               .addJavadoc(WaiterDocs.waiterMethodInClient(
                                                   poetExtensions.getAsyncWaiterInterface()));
        
        type.addMethod(waiterOperationBody(builder).build());
    }

    @Override
    public ClassName className() {
        return className;
    }

    private String getJavadoc() {
        return "Service client for accessing " + model.getMetadata().getDescriptiveServiceName() + " asynchronously. This can be "
               + "created using the static {@link #builder()} method.\n\n" + model.getMetadata().getDocumentation();
    }

    private MethodSpec create() {
        return MethodSpec.methodBuilder("create")
                         .returns(className)
                         .addModifiers(STATIC, PUBLIC)
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
                         .addModifiers(STATIC, PUBLIC)
                         .addJavadoc("Create a builder that can be used to configure and create a {@link $T}.", className)
                         .addStatement("return new $T()", builderClass)
                         .build();
    }

    /**
     * @return List generated of methods for all operations.
     */
    protected Iterable<MethodSpec> operations() {
        return model.getOperations().values().stream()
                    .flatMap(this::operationsAndSimpleMethods)
                    .sorted(Comparator.comparing(m -> m.name))
                    .collect(toList());
    }

    private Stream<MethodSpec> operationsAndSimpleMethods(OperationModel operationModel) {
        List<MethodSpec> methods = new ArrayList<>();
        methods.addAll(traditionalMethods(operationModel));
        methods.addAll(overloadMethods(operationModel));
        methods.addAll(paginatedMethods(operationModel));
        return methods.stream()
                      // Add Deprecated annotation if needed to all overloads
                      .map(m -> DeprecationUtils.checkDeprecated(operationModel, m));
    }

    private List<MethodSpec> paginatedMethods(OperationModel opModel) {
        List<MethodSpec> methods = new ArrayList<>();

        if (opModel.isPaginated()) {
            if (opModel.getInputShape().isSimpleMethod()) {
                methods.add(paginatedSimpleMethod(opModel));
            }

            MethodSpec paginatedMethod = paginatedTraditionalMethod(opModel);
            methods.add(paginatedMethod);

            String consumerBuilderJavadoc = consumerBuilderJavadoc(opModel, SimpleMethodOverload.PAGINATED);
            methods.add(ClientClassUtils.consumerBuilderVariant(paginatedMethod, consumerBuilderJavadoc));
        }

        return methods;
    }

    protected MethodSpec paginatedTraditionalMethod(OperationModel opModel) {
        String methodName = PaginatorUtils.getPaginatedMethodName(opModel.getMethodName());
        ClassName requestType = ClassName.get(modelPackage, opModel.getInput().getVariableType());
        ClassName responsePojoType = poetExtensions.getResponseClassForPaginatedAsyncOperation(opModel.getOperationName());

        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                                               .returns(responsePojoType)
                                               .addParameter(requestType, opModel.getInput().getVariableName())
                                               .addJavadoc(opModel.getDocs(model,
                                                                           ClientType.ASYNC,
                                                                           SimpleMethodOverload.PAGINATED));

        return paginatedMethodBody(builder, opModel).build();
    }

    protected MethodSpec.Builder paginatedMethodBody(MethodSpec.Builder builder, OperationModel operationModel) {
        return builder.addModifiers(DEFAULT, PUBLIC)
                      .addStatement("throw new $T()", UnsupportedOperationException.class);
    }

    private MethodSpec paginatedSimpleMethod(OperationModel opModel) {
        String methodName = PaginatorUtils.getPaginatedMethodName(opModel.getMethodName());
        ClassName requestType = ClassName.get(modelPackage, opModel.getInput().getVariableType());
        ClassName responsePojoType = poetExtensions.getResponseClassForPaginatedAsyncOperation(opModel.getOperationName());

        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                                               .addModifiers(DEFAULT, PUBLIC)
                                               .returns(responsePojoType)
                                               .addStatement("return $L($T.builder().build())", methodName, requestType)
                                               .addJavadoc(opModel.getDocs(model,
                                                                           ClientType.ASYNC,
                                                                           SimpleMethodOverload.NO_ARG_PAGINATED));

        return builder.build();
    }

    /**
     * @param opModel Operation to generate simple methods for.
     * @return All simple method overloads for a given operation.
     */
    private List<MethodSpec> overloadMethods(OperationModel opModel) {
        String consumerBuilderFileJavadoc = consumerBuilderJavadoc(opModel, SimpleMethodOverload.FILE);

        List<MethodSpec> methodOverloads = new ArrayList<>();
        if (opModel.getInputShape().isSimpleMethod()) {
            methodOverloads.add(noArgSimpleMethod(opModel));
        }
        if (opModel.hasStreamingInput() && opModel.hasStreamingOutput()) {
            MethodSpec streamingMethod = streamingInputOutputFileSimpleMethod(opModel);
            methodOverloads.add(streamingMethod);
            methodOverloads.add(ClientClassUtils.consumerBuilderVariant(streamingMethod, consumerBuilderFileJavadoc));
        } else if (opModel.hasStreamingInput()) {
            MethodSpec streamingInputMethod = streamingInputFileSimpleMethod(opModel);
            methodOverloads.add(streamingInputMethod);
            methodOverloads.add(ClientClassUtils.consumerBuilderVariant(streamingInputMethod, consumerBuilderFileJavadoc));
        } else if (opModel.hasStreamingOutput()) {
            MethodSpec streamingOutputMethod = streamingOutputFileSimpleMethod(opModel);
            methodOverloads.add(streamingOutputMethod);
            methodOverloads.add(ClientClassUtils.consumerBuilderVariant(streamingOutputMethod, consumerBuilderFileJavadoc));
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
        return builder.addModifiers(DEFAULT, PUBLIC)
                      .addStatement("throw new $T()", UnsupportedOperationException.class);
    }

    /**
     * Generates the traditional method for an operation (i.e. one that takes a request and returns a response).
     */
    private List<MethodSpec> traditionalMethods(OperationModel opModel) {
        List<MethodSpec> methods = new ArrayList<>();

        methods.add(traditionalMethod(opModel));

        String consumerBuilderJavadoc = consumerBuilderJavadoc(opModel, SimpleMethodOverload.NORMAL);
        methods.add(ClientClassUtils.consumerBuilderVariant(methods.get(0), consumerBuilderJavadoc));

        return methods;
    }

    protected MethodSpec traditionalMethod(OperationModel opModel) {
        ClassName responsePojoType = getPojoResponseType(opModel);
        ClassName requestType = ClassName.get(modelPackage, opModel.getInput().getVariableType());

        MethodSpec.Builder builder = methodSignatureWithReturnType(opModel)
                .addParameter(requestType, opModel.getInput().getVariableName())
                .addJavadoc(opModel.getDocs(model, ClientType.ASYNC));

        if (opModel.hasStreamingInput()) {
            builder.addParameter(ClassName.get(AsyncRequestBody.class), ASYNC_STREAMING_INPUT_PARAM);
        } else if (opModel.hasEventStreamInput()) {
            String eventStreamShapeName = EventStreamUtils.getEventStreamInRequest(opModel.getInputShape())
                                                          .getShapeName();
            ClassName shapeClass = ClassName.get(modelPackage, eventStreamShapeName);
            ParameterizedTypeName requestPublisher = ParameterizedTypeName.get(ClassName.get(Publisher.class), shapeClass);
            builder.addParameter(requestPublisher, EVENT_PUBLISHER_PARAM_NAME);
        }

        if (opModel.hasStreamingOutput()) {
            builder.addTypeVariable(STREAMING_TYPE_VARIABLE);
            ParameterizedTypeName asyncResponseHandlerType = ParameterizedTypeName
                .get(ClassName.get(AsyncResponseTransformer.class), responsePojoType, STREAMING_TYPE_VARIABLE);
            builder.addParameter(asyncResponseHandlerType, "asyncResponseTransformer");
        } else if (opModel.hasEventStreamOutput()) {
            builder.addParameter(poetExtensions.eventStreamResponseHandlerType(opModel), EVENT_RESPONSE_HANDLER_PARAM_NAME);
        }
        return operationBody(builder, opModel).build();
    }

    /**
     * Generate a simple method that takes no arguments for operations with no required parameters.
     */
    private MethodSpec noArgSimpleMethod(OperationModel opModel) {
        return interfaceMethodSignature(opModel)
                .addJavadoc(opModel.getDocs(model, ClientType.ASYNC, SimpleMethodOverload.NO_ARG))
                .addStatement("return $N($N.builder().build())",
                              opModel.getMethodName(),
                              opModel.getInput().getVariableType())
                .build();
    }

    /**
     * Generate a simple method for operations with a streaming input member that takes a {@link Path} containing the data
     * to upload.
     */
    private MethodSpec streamingInputFileSimpleMethod(OperationModel opModel) {
        ClassName requestType = ClassName.get(modelPackage, opModel.getInput().getVariableType());
        return interfaceMethodSignature(opModel)
                .addJavadoc(opModel.getDocs(model, ClientType.ASYNC, SimpleMethodOverload.FILE))
                .addParameter(requestType, opModel.getInput().getVariableName())
                .addParameter(ClassName.get(Path.class), "sourcePath")
                .addStatement("return $L($L, $T.fromFile(sourcePath))", opModel.getMethodName(),
                              opModel.getInput().getVariableName(),
                              ClassName.get(AsyncRequestBody.class))
                .build();
    }

    /**
     * Generate a simple method for operations with a streaming output member that takes a {@link Path} where data
     * will be downloaded to.
     */
    private MethodSpec streamingOutputFileSimpleMethod(OperationModel opModel) {
        ClassName requestType = ClassName.get(modelPackage, opModel.getInput().getVariableType());
        return interfaceMethodSignature(opModel)
                .returns(completableFutureType(getPojoResponseType(opModel)))
                .addJavadoc(opModel.getDocs(model, ClientType.ASYNC, SimpleMethodOverload.FILE))
                .addParameter(requestType, opModel.getInput().getVariableName())
                .addParameter(ClassName.get(Path.class), "destinationPath")
                .addStatement("return $L($L, $T.toFile(destinationPath))", opModel.getMethodName(),
                              opModel.getInput().getVariableName(),
                              ClassName.get(AsyncResponseTransformer.class))
                .build();
    }

    /**
     * Generate a simple method for operations with streaming input and output members.
     * Streaming input member takes a {@link Path} containing the data to upload and
     * the streaming output member takes a {@link Path} where data will be downloaded to.
     */
    private MethodSpec streamingInputOutputFileSimpleMethod(OperationModel opModel) {
        ClassName requestType = ClassName.get(modelPackage, opModel.getInput().getVariableType());
        return interfaceMethodSignature(opModel)
            .returns(completableFutureType(getPojoResponseType(opModel)))
            .addJavadoc(opModel.getDocs(model, ClientType.ASYNC, SimpleMethodOverload.FILE))
            .addParameter(requestType, opModel.getInput().getVariableName())
            .addParameter(ClassName.get(Path.class), "sourcePath")
            .addParameter(ClassName.get(Path.class), "destinationPath")
            .addStatement("return $L($L, $T.fromFile(sourcePath), $T.toFile(destinationPath))",
                          opModel.getMethodName(),
                          opModel.getInput().getVariableName(),
                          ClassName.get(AsyncRequestBody.class),
                          ClassName.get(AsyncResponseTransformer.class))
            .build();
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
            .addModifiers(PUBLIC, DEFAULT);
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
        } else if (opModel.hasEventStreamOutput()) {
            // Event streaming doesn't support transforming into a result type so it just returns void.
            return completableFutureType(ClassName.get(Void.class));
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

    private String consumerBuilderJavadoc(OperationModel opModel, SimpleMethodOverload overload) {
        return opModel.getDocs(model, ClientType.ASYNC, overload, new DocConfiguration().isConsumerBuilder(true));
    }

    protected MethodSpec utilitiesMethod() {
        UtilitiesMethod config = model.getCustomizationConfig().getUtilitiesMethod();
        ClassName returnType = PoetUtils.classNameFromFqcn(config.getReturnType());

        MethodSpec.Builder builder = MethodSpec.methodBuilder(UtilitiesMethod.METHOD_NAME)
                                               .returns(returnType)
                                               .addModifiers(PUBLIC)
                                               .addJavadoc("Creates an instance of {@link $T} object with the "
                                                           + "configuration set on this client.", returnType);
        return utilitiesOperationBody(builder).build();
    }

    private MethodSpec additionalBuilders(AdditionalBuilderMethod additionalMethod) {

        String methodName = Validate.paramNotNull(additionalMethod.getMethodName(), "methodName");
        ClassName returnType = PoetUtils.classNameFromFqcn(
            Validate.paramNotNull(additionalMethod.getReturnType(), "returnType"));
        ClassName instanceType = PoetUtils.classNameFromFqcn(
            Validate.paramNotNull(additionalMethod.getInstanceType(), "instanceType"));

        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                                               .returns(returnType)
                                               .addModifiers(STATIC, PUBLIC)
                                               .addJavadoc(additionalMethod.getJavaDoc())
                                               .addStatement("return $T.$L", instanceType, additionalMethod.getStatement());

        return builder.build();
    }

    protected MethodSpec.Builder utilitiesOperationBody(MethodSpec.Builder builder) {
        return builder.addModifiers(DEFAULT).addStatement("throw new $T()", UnsupportedOperationException.class);
    }

    protected MethodSpec.Builder waiterOperationBody(MethodSpec.Builder builder) {
        return builder.addModifiers(DEFAULT, PUBLIC)
                      .addStatement("throw new $T()", UnsupportedOperationException.class);
    }
}
