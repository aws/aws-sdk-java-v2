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

package software.amazon.awssdk.codegen.poet.batchmanager;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getBatchRequestEntryType;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getBatchRequestMethod;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getBatchRequestType;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getBatchResponseType;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getDestinationMethod;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getErrorBatchEntry;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getErrorCodeMethod;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getErrorEntriesMethod;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getRequestType;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getResponseType;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getSuccessBatchEntry;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getSuccessEntriesMethod;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getType;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.codegen.model.config.customization.BatchFunctionsTypes;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.batchmanager.BatchAndSend;
import software.amazon.awssdk.core.batchmanager.BatchResponseMapper;
import software.amazon.awssdk.core.batchmanager.IdentifiableMessage;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Logger;

public class BatchFunctionsClassSpec implements ClassSpec {

    private static final Logger log = Logger.loggerFor(BatchFunctionsClassSpec.class);

    private final IntermediateModel model;
    private final Map<String, BatchFunctionsTypes> batchFunctions;
    private final String modelPackage;
    private final PoetExtensions poetExtensions;
    private final ClassName className;
    private final ClassName clientName;
    private final ClassName asyncClientName;

    public BatchFunctionsClassSpec(IntermediateModel model) {
        this.model = model;
        this.batchFunctions = model.getCustomizationConfig().getBatchManager().getBatchableFunctions();
        this.modelPackage = model.getMetadata().getFullModelPackageName();
        this.poetExtensions = new PoetExtensions(model);
        this.className = poetExtensions.getBatchFunctionsClass();
        this.clientName = poetExtensions.getClientClass(model.getMetadata().getSyncInterface());
        this.asyncClientName = poetExtensions.getClientClass(model.getMetadata().getAsyncInterface());
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder classBuilder = PoetUtils.createClassBuilder(className)
                                                 .addAnnotation(SdkInternalApi.class)
                                                 .addModifiers(FINAL)
                                                 .addMethod(constructor())
                                                 .addMethods(batchFunctions());

        return classBuilder.build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder().addModifiers(PRIVATE).build();
    }

    private List<MethodSpec> batchFunctions() {
        return batchFunctions.entrySet()
                             .stream()
                             .flatMap(this::batchFunctions)
                             .sorted(Comparator.comparing(m -> m.name))
                             .collect(Collectors.toList());
    }

    private Stream<MethodSpec> batchFunctions(Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        List<MethodSpec> methods = new ArrayList<>();
        methods.addAll(batchingFunction(batchFunctions));
        methods.addAll(responseMapper(batchFunctions));
        return methods.stream();
    }

    private List<MethodSpec> batchingFunction(Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(batchingFunctionSync(batchFunctions));
        methods.add(batchingFunctionAsync(batchFunctions));
        methods.add(batchingFunctionHelper(batchFunctions));
        methods.add(addCreateBatchEntryMethod(batchFunctions));
        return methods;
    }

    private MethodSpec batchingFunctionSync(Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        String methodName = batchFunctions.getKey() + "BatchFunction";
        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, batchingFunctionReturn(batchFunctions))
            .addModifiers(PUBLIC, STATIC)
            .addParameter(clientName, "client")
            .addParameter(ClassName.get(Executor.class), "executor");
        addBatchFunctionStatementSync(builder, batchFunctions);

        return builder.build();
    }

    private MethodSpec batchingFunctionAsync(Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        String methodName = batchFunctions.getKey() + "AsyncBatchFunction";
        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, batchingFunctionReturn(batchFunctions))
            .addModifiers(PUBLIC, STATIC)
            .addParameter(asyncClientName, "client");
        addBatchFunctionStatementAsync(builder, batchFunctions);

        return builder.build();
    }

    private MethodSpec batchingFunctionHelper(Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        ClassName returnType = getBatchRequestType(batchFunctions, modelPackage);
        ClassName batchRequestEntryType = getBatchRequestEntryType(batchFunctions, modelPackage);
        ClassName batchRequestType = getBatchRequestType(batchFunctions, modelPackage);
        ParameterizedTypeName identifiedRequest = ParameterizedTypeName.get(ClassName.get(IdentifiableMessage.class),
                                                                            getRequestType(batchFunctions, modelPackage));
        ParameterizedTypeName requestsParam = ParameterizedTypeName.get(ClassName.get(List.class), identifiedRequest);
        String methodName = "create" + returnType.simpleName();

        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, returnType)
            .addModifiers(PRIVATE, STATIC)
            .addParameter(requestsParam, "identifiedRequests")
            .addParameter(ClassName.get(String.class), "batchKey");

        String destinationMethod = getDestinationMethod(batchFunctions);
        builder.addStatement("$T entries = identifiedRequests.stream()\n"
                             + ".map(identifiedRequest -> $N(identifiedRequest.id(),\n"
                             + "identifiedRequest.message()))\n"
                             + ".collect($T.toList())",
                             ParameterizedTypeName.get(ClassName.get(List.class), batchRequestEntryType),
                             addCreateBatchEntryMethod(batchFunctions),
                             ClassName.get(Collectors.class))
               .addCode("// Since requests are batched together according to a combination of their queueUrl and "
                        + "overrideConfiguration, all requests must have the same overrideConfiguration so it is sufficient to "
                        + "retrieve it from the first request.\n")
               .addStatement("$T overrideConfiguration = identifiedRequests\n"
                             + ".get(0)\n"
                             + ".message()\n"
                             + ".overrideConfiguration()",
                             ParameterizedTypeName.get(ClassName.get(Optional.class),
                                                       ClassName.get(AwsRequestOverrideConfiguration.class)))
               .addStatement("return overrideConfiguration.map(overrideConfig -> $T.builder()\n"
                             + ".$L(batchKey)\n"
                             + ".overrideConfiguration(overrideConfig)\n"
                             + ".entries(entries)\n"
                             + ".build())\n"
                             + ".orElse($T.builder()\n"
                             + ".$L(batchKey)\n"
                             + ".entries(entries)\n"
                             + ".build())",
                             batchRequestType, destinationMethod,
                             batchRequestType, destinationMethod);

        return builder.build();
    }

    private void addBatchFunctionStatementSync(MethodSpec.Builder builder,
                                               Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        addBatchFunctionStatementCore(builder, batchFunctions);
        builder.addStatement("    return $T.supplyAsync(() -> client.$L(batchRequest), executor);\n"
                             + "}",
                             ClassName.get(CompletableFuture.class),
                             getBatchRequestMethod(batchFunctions));
    }

    private void addBatchFunctionStatementAsync(MethodSpec.Builder builder,
                                                Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        addBatchFunctionStatementCore(builder, batchFunctions);
        builder.addStatement("    return client.$L(batchRequest);\n"
                             + "}",
                             getBatchRequestMethod(batchFunctions));
    }

    private void addBatchFunctionStatementCore(MethodSpec.Builder builder,
                                                      Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        builder.addStatement("return (identifiedRequests, batchKey) -> {\n"
                             + "    $T batchRequest = $N(identifiedRequests, batchKey)",
                             getBatchRequestType(batchFunctions, modelPackage),
                             batchingFunctionHelper(batchFunctions));
    }

    private MethodSpec addCreateBatchEntryMethod(Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        ClassName batchRequestEntryType = getBatchRequestEntryType(batchFunctions, modelPackage);
        ClassName requestType = getRequestType(batchFunctions, modelPackage);
        String methodName = "create" + batchRequestEntryType.simpleName();

        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, batchRequestEntryType)
            .addModifiers(PRIVATE, STATIC)
            .addParameter(ClassName.get(String.class), "id")
            .addParameter(getRequestType(batchFunctions, modelPackage), "request");
        builder.addCode("return ");
        builderMapOneClassToAnother(batchRequestEntryType, requestType, builder);
        builder.addCode(".build();");

        return builder.build();
    }

    private ParameterizedTypeName batchingFunctionReturn(Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        ClassName requestClass = getRequestType(batchFunctions, modelPackage);
        ClassName batchResponseClass = getBatchResponseType(batchFunctions, modelPackage);

        return ParameterizedTypeName.get(ClassName.get(BatchAndSend.class),
                                         requestClass, batchResponseClass);
    }

    private List<MethodSpec> responseMapper(Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(responseMapperCore(batchFunctions));
        methods.add(createResponseFromEntry(batchFunctions));
        if (getErrorEntriesMethod(batchFunctions) != null) {
            methods.add(createThrowableFromEntry(batchFunctions));
        }
        return methods;
    }

    private MethodSpec responseMapperCore(Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        String methodName = batchFunctions.getKey() + "ResponseMapper";
        ClassName responseClass = getResponseType(batchFunctions, modelPackage);
        ClassName batchResponseClass = getBatchResponseType(batchFunctions, modelPackage);
        ParameterizedTypeName returnType = ParameterizedTypeName.get(ClassName.get(BatchResponseMapper.class),
                                                                     batchResponseClass, responseClass);
        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, returnType)
            .addModifiers(PUBLIC, STATIC);

        ParameterizedTypeName identifiedResponse = ParameterizedTypeName.get(ClassName.get(IdentifiableMessage.class),
                                                                             responseClass);
        ParameterizedTypeName identifiedThrowable = ParameterizedTypeName.get(ClassName.get(IdentifiableMessage.class),
                                                                              ClassName.get(Throwable.class));
        ParameterizedTypeName either = ParameterizedTypeName.get(ClassName.get(Either.class), identifiedResponse,
                                                                 identifiedThrowable);
        ParameterizedTypeName mappedResponsesType = ParameterizedTypeName.get(ClassName.get(List.class), either);
        builder.addStatement("return batchResponse -> {\n"
                             + "    $T mappedResponses = new $T<>()",
                             mappedResponsesType, ClassName.get(ArrayList.class))
               .addStatement("batchResponse.$L()\n"
                             + ".forEach(batchResponseEntry -> {\n"
                             + "    IdentifiableMessage<$T> response = $N(batchResponseEntry, batchResponse);\n"
                             + "    mappedResponses.add(Either.left(response));\n"
                             + "})",
                             getSuccessEntriesMethod(batchFunctions), responseClass,
                             createResponseFromEntry(batchFunctions));

        if (getErrorEntriesMethod(batchFunctions) != null) {
            builder.addStatement("batchResponse.$L()\n"
                                 + ".forEach(batchResponseEntry -> {\n"
                                 + "    IdentifiableMessage<Throwable> response = $N(batchResponseEntry);\n"
                                 + "    mappedResponses.add(Either.right(response));\n"
                                 + "})",
                                 getErrorEntriesMethod(batchFunctions),
                                 createThrowableFromEntry(batchFunctions));
        }
        builder.addStatement("return mappedResponses;\n"
                             + "}");

        return builder.build();
    }

    private MethodSpec createResponseFromEntry(Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        String methodName = "create" + getResponseType(batchFunctions, modelPackage).simpleName();
        ClassName responseClass = getResponseType(batchFunctions, modelPackage);
        ClassName responseEntryClass = getSuccessBatchEntry(batchFunctions, modelPackage);
        ParameterizedTypeName returnType = ParameterizedTypeName.get(ClassName.get(IdentifiableMessage.class), responseClass);
        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, returnType)
            .addModifiers(PRIVATE, STATIC)
            .addParameter(responseEntryClass, "successfulEntry")
            .addParameter(getBatchResponseType(batchFunctions, modelPackage), "batchResponse");

        // TODO: Need to modify somehow for services like dynamoDB since it returns a map of items, so the successfulEntry
        //  parameter is actually a Map.Entry as opposed to a regular entry.
        // TODO: Also need to modify since for some services, entries don't have an id() method.
        ClassName responseBuilderClass = getType(responseClass.simpleName() + ".Builder", modelPackage);
        builder.addStatement("String key = successfulEntry.id()");
        builder.addCode("$T builder = ",
                        responseBuilderClass);
        builderMapOneClassToAnother(responseClass, responseEntryClass, builder);
        builder.addCode(";\n");
        builder.beginControlFlow("if (batchResponse.responseMetadata() != null)")
               .addStatement("builder.responseMetadata(batchResponse.responseMetadata())")
               .endControlFlow();
        builder.beginControlFlow("if (batchResponse.sdkHttpResponse() != null)")
               .addStatement("builder.sdkHttpResponse(batchResponse.sdkHttpResponse())")
               .endControlFlow();
        builder.addStatement("$T response = builder.build()", responseClass);
        builder.addStatement("return new $T(key, response)",
                             returnType);

        return builder.build();
    }

    private MethodSpec createThrowableFromEntry(Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        String methodName = "createThrowable";
        ClassName responseClass = ClassName.get(Throwable.class);
        ParameterizedTypeName returnClass = ParameterizedTypeName.get(ClassName.get(IdentifiableMessage.class), responseClass);
        ClassName responseEntryClass = getErrorBatchEntry(batchFunctions, modelPackage);
        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, returnClass)
            .addModifiers(PRIVATE, STATIC)
            .addParameter(responseEntryClass, "failedEntry");

        ClassName newType = getType(model.getMetadata().getServiceName() + "Exception", modelPackage);
        builder.addStatement("String key = failedEntry.id()");
        builder.addCode("$T.Builder builder = ", newType);
        builderMapOneClassToAnother(newType, responseEntryClass, builder);
        builder.addCode(";\n");
        builder.addStatement("builder.statusCode($T.parseInt(failedEntry.$L()))",
                             ClassName.get(Integer.class), getErrorCodeMethod(batchFunctions));

        builder.addStatement("$T response = builder.build()", responseClass);
        builder.addStatement("return new $T(key, response)",
                             returnClass);

        return builder.build();
    }

    private void builderMapOneClassToAnother(ClassName newType, ClassName originalType,
                                             MethodSpec.Builder builder) {
        builder.addCode("$T.builder()", newType);
        try {
            // TODO: The problem is here, it thinks the batchRequestEntryType (for Sqs, SendMessageBatchRequestEntry) does not
            //  exist. I tried putting this in while loop to wait until the models are generated but that didn't work. Tried
            //  overriding the  compute method in ModelClassGeneratorTasks to only generate BatchFunctions once the models are
            //  generated but that didn't work either.
            Class<?> newClass = Class.forName(newType.canonicalName()); // Even this fails, so not a problem with the builder.
            Class<?> newClassBuilder = Class.forName(newType.canonicalName() + "$Builder"); // Using $ or . both result in errors.

            Class<?> originalClass = Class.forName(originalType.canonicalName());
            builder.addStatement("$T", originalClass);
            Arrays.stream(newClassBuilder.getMethods())
                  .forEach(method -> {
                      try {
                          originalClass.getMethod(method.getName(), method.getParameterTypes());

                          // TODO: Not all services have an id method, but the ID method is kind of integral to how the
                          //  responseMapper or even the default manager works to correlate requests and responses. The solution
                          //  will either involves refactoring the core batchManager to correlate requests to responses some
                          //  other way or by changing the services to all include an id field/method in batchRequest and
                          //  batchResponse entries.
                          if (method.getName().equals("id")) {
                              builder.addCode(".$S(id)", method.getName());
                          } else {
                              builder.addCode(".$S(request.$S())", method.getName(), method.getName());
                          }
                      } catch (NoSuchMethodException e) {
                          // If the method exists in the batchRequestEntryClassBuilder but not the requestClass, then do
                          // nothing since we can't add anything for that builder method.
                      } catch (SecurityException e) {
                          log.warn(() -> String.valueOf(e));
                      }
                  });
        } catch (ClassNotFoundException e) {
            log.warn(() -> "Error generating createBatchEntryMethod. " + e);
        }
    }

    private MethodSpec.Builder methodSignatureWithReturnType(String methodName, TypeName returnType) {
        return MethodSpec.methodBuilder(methodName)
                         .returns(returnType);
    }

}
