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

package software.amazon.awssdk.codegen.docs;

import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getBatchRequestType;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getRequestType;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getResponseType;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.codegen.model.config.customization.BatchManagerMethods;

public final class BatchManagerDocs {

    private static final String BUILDER_RETURN_DOC = "a reference to this object so that method calls can be chained together.";

    private BatchManagerDocs() {
    }

    public static CodeBlock batchManagerSyncInterfaceDocs(String serviceName, boolean isSync) {
        String syncOrAsync = isSync ? "sync" : "async";
        String javaDoc = " Batch manager class that implements automatic batching features for a $N $L client. This can be "
                         + "created using the static {@link #builder()} method.\n"
                         + "<p>\n"
                         + "The batch manager's automatic batching features allows for request batching using client-side "
                         + "buffering. This means that calls made from the client are first buffered and then sent as a batch "
                         + "request to the service. Client side buffering allows buffering a number of requests up to a service "
                         + "or user defined limit before being sent as a batch request. Outgoing calls are also periodically "
                         + "flushed after a defined period of time if batch requests do not reach the defined batch size limit.";
        return CodeBlock.builder()
                        .add(javaDoc, serviceName, syncOrAsync)
                        .build();
    }

    public static CodeBlock batchMethodDocs(String serviceName, Map.Entry<String, BatchManagerMethods> batchFunctions,
                                            String modelPackage) {
        String batchKey = batchFunctions.getValue().getBatchKey();
        String returnStatement = "CompletableFuture of the corresponding {@link $T}";
        ClassName responseType = getResponseType(batchFunctions, modelPackage);
        ClassName requestType = getRequestType(batchFunctions, modelPackage);
        ClassName batchRequestType = getBatchRequestType(batchFunctions, modelPackage);

        String description = "Buffers outgoing {@link $T}s on the client and sends them as a "
                             + "{@link $T} to $L. Requests are batched together according to a batchKey "
                             + "calculated from the request's $L and overrideConfiguration which are then "
                             + "sent periodically to $L. If the number of requests for a batchKey reaches or "
                             + "exceeds the configured max items, then the requests are immediately flushed and "
                             + "the timeout on the periodic flush is reset.";

        String javadocs = new DocumentationBuilder().description(description)
                                                    .param("request", "the outgoing " + requestType.simpleName())
                                                    .returns(returnStatement)
                                                    .build();
        return CodeBlock.builder()
                        .add(javadocs, requestType, batchRequestType, serviceName, batchKey, serviceName, responseType)
                        .build();

    }

    public static CodeBlock batchManagerBuilderMethodJavadoc(ClassName className) {
        String javadocs = new DocumentationBuilder()
            .description("Create a builder that can be used to configure and create a {@link $T}.")
            .returns("a builder")
            .build();

        return CodeBlock.builder()
                        .add(javadocs, className)
                        .build();
    }

    public static CodeBlock batchManagerBuilderClientJavadoc(ClassName clientClass) {
        String javadocs = new DocumentationBuilder()
            .description("Sets a custom {@link $T} that will be used to poll the resource.\n"
                         + "<p>"
                         + "This client must be closed by the caller when it is ready to be disposed. The SDK will not "
                         + "close the client when the BatchManager is closed.")
            .param("client", "the client used to send and receive batch messages.")
            .returns(BUILDER_RETURN_DOC)
            .build();

        return CodeBlock.builder()
                        .add(javadocs, clientClass)
                        .build();
    }

    public static CodeBlock batchManagerBuilderPollingStrategy() {
        String javadocs = new DocumentationBuilder()
            .description("Defines overrides to the default BatchManager configuration.")
            .param("overrideConfiguration", "the override configuration to set")
            .returns(BUILDER_RETURN_DOC)
            .build();

        return CodeBlock.builder()
                        .add(javadocs)
                        .build();
    }

    public static CodeBlock batchManagerBuilderScheduledExecutorServiceJavadoc(String className) {
        String javadocs = new DocumentationBuilder()
            .description("Sets a custom {@link $T} that will be used to schedule periodic buffer flushes.\n "
                         + "<p>\n Creating a $L directly from the client will use the client's scheduled executor."
                         + " If supplied by the user, this {@link ScheduledExecutorService} must be closed by the caller when "
                         + "it is ready to be shut down.")
            .param("scheduledExecutor", "the scheduledExecutor to be used")
            .returns(BUILDER_RETURN_DOC)
            .build();

        return CodeBlock.builder()
                        .add(javadocs, ClassName.get(ScheduledExecutorService.class), className)
                        .build();
    }

    public static CodeBlock batchManagerBuilderExecutorJavadoc(String className) {
        String javadocs = new DocumentationBuilder()
            .description("Sets a custom {@link $T} that will be used execute client requests asynchronously.\n "
                         + "<p>\n Creating a $L directly from the client will use the client's executor. If supplied by the "
                         + "user, this {@link Executor} must be closed by the caller when it is ready to be shut down.")
            .param("executor", "the executor to be used")
            .returns(BUILDER_RETURN_DOC)
            .build();

        return CodeBlock.builder()
                        .add(javadocs, ClassName.get(Executor.class), className)
                        .build();
    }
}
