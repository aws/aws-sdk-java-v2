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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.WaiterDefinition;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;

public final class WaiterDocs {

    private WaiterDocs() {
    }

    public static String waiterInterfaceJavadoc() {
        return "Waiter utility class that polls a resource until a desired state is reached or until it is "
               + "determined that the resource will never enter into the desired state. This can be"
               + " created using the static {@link #builder()} method";
    }

    public static CodeBlock waiterOperationJavadoc(ClassName className, Map.Entry<String, WaiterDefinition> waiterDefinition,
                                                   OperationModel operationModel) {
        String returnStatement = "WaiterResponse containing either a response or an exception that has matched with the waiter "
                                 + "success condition";

        String asyncReturnStatement = "CompletableFuture containing the WaiterResponse. It completes successfully when the "
                                      + "resource enters into a desired state or exceptionally when it is determined that the "
                                      + "resource will never enter into the desired state.";
        String javadocs = new DocumentationBuilder().description("Polls {@link $T#$N} API until the desired condition "
                                                                 + "{@code $N} is met, "
                                                                 + "or until it is determined that the resource will never "
                                                                 + "enter into the desired state")
                                                    .param(operationModel.getInput().getVariableName(), "the request to be used"
                                                                                                        + " for polling")
                                                    .returns(className.simpleName().contains("Async") ?
                                                             asyncReturnStatement :
                                                             returnStatement)
                                                    .build();
        return CodeBlock.builder()
                        .add(javadocs, className, operationModel.getMethodName(), waiterDefinition.getKey())
                        .build();

    }

    public static CodeBlock waiterOperationConsumerBuilderJavadoc(ClassName clientClassName,
                                                                  ClassName requestClassName,
                                                                  Map.Entry<String, WaiterDefinition> waiterDefinition,
                                                                  OperationModel operationModel) {
        String javadocs = new DocumentationBuilder().description("Polls {@link $T#$N} API until the desired condition "
                                                                 + "{@code $N} is met, "
                                                                 + "or until it is determined that the resource will never "
                                                                 + "enter into the desired state. \n "
                                                                 + "<p>This is a convenience method to create an instance of "
                                                                 + "the request builder without the need "
                                                                 + "to create one manually using {@link $T#builder()} ")
                                                    .param(operationModel.getInput().getVariableName(), "The consumer that will"
                                                                                                        + " configure the "
                                                                                                        + "request to be used"
                                                                                                        + " for polling")
                                                    .returns(clientClassName.simpleName().contains("Async") ?
                                                             "CompletableFuture of the WaiterResponse containing either a "
                                                             + "response or an exception that has matched with the waiter "
                                                             + "success condition"
                                                             : "WaiterResponse containing either a response or an exception that"
                                                             + " has matched with the waiter success condition")
                                                    .build();
        return CodeBlock.builder()
                        .add(javadocs, clientClassName, operationModel.getMethodName(), waiterDefinition.getKey(),
                             requestClassName)
                        .build();
    }

    public static CodeBlock waiterBuilderMethodJavadoc(ClassName className) {
        String javadocs = new DocumentationBuilder()
            .description("Create a builder that can be used to configure and create a {@link $T}.")
            .returns("a builder")
            .build();

        return CodeBlock.builder()
                        .add(javadocs, className)
                        .build();
    }

    public static CodeBlock waiterCreateMethodJavadoc(ClassName waiterClassName, ClassName clientClassName) {
        String javadocs = new DocumentationBuilder()
            .description("Create an instance of {@link $T} with the default configuration. \n"
                         + "<p><b>A default {@link $T} will be created to poll resources. It is recommended "
                         + "to share a single instance of the waiter created via this method. If it is not desirable "
                         + "to share a waiter instance, invoke {@link #close()} to release the resources once the waiter"
                         + " is not needed.</b>")
            .returns("an instance of {@link $T}")
            .build();

        return CodeBlock.builder()
                        .add(javadocs, waiterClassName, clientClassName, waiterClassName)
                        .build();
    }

    public static CodeBlock waiterBuilderPollingStrategy() {
        String javadocs = new DocumentationBuilder()
            .description("Defines overrides to the default SDK waiter configuration that should be used for waiters created "
                         + "from this builder")
            .param("overrideConfiguration", "the override configuration to set")
            .returns("a reference to this object so that method calls can be chained together.")
            .build();

        return CodeBlock.builder()
                        .add(javadocs)
                        .build();
    }

    public static CodeBlock waiterBuilderPollingStrategyConsumerBuilder() {
        String javadocs = new DocumentationBuilder()
            .description("This is a convenient method to pass the override configuration without the need to "
                         + "create an instance manually via {@link $T#builder()}")
            .param("overrideConfiguration", "The consumer that will configure the overrideConfiguration")
            .see("#overrideConfiguration(WaiterOverrideConfiguration)")
            .returns("a reference to this object so that method calls can be chained together.")
            .build();

        return CodeBlock.builder()
                        .add(javadocs, ClassName.get(WaiterOverrideConfiguration.class))
                        .build();
    }

    public static CodeBlock waiterBuilderScheduledExecutorServiceJavadoc() {
        String javadocs = new DocumentationBuilder()
            .description("Sets a custom {@link $T} that will be used to schedule async polling attempts \n "
                         + "<p> This executorService must be closed by the caller when it is ready to be disposed. The"
                         + " SDK will not close the executorService when the waiter is closed")
            .param("executorService", "the executorService to set")
            .returns("a reference to this object so that method calls can be chained together.")
            .build();

        return CodeBlock.builder()
                        .add(javadocs, ClassName.get(ScheduledExecutorService.class))
                        .build();
    }

    public static CodeBlock waiterBuilderClientJavadoc(ClassName className) {
        String javadocs = new DocumentationBuilder()
            .description("Defines the {@link $T} to use when polling a resource")
            .description("Sets a custom {@link $T} that will be used to poll the resource \n "
                         + "<p> This SDK client must be closed by the caller when it is ready to be disposed. The"
                         + " SDK will not close the client when the waiter is closed")
            .param("client", "the client to send the request")
            .returns("a reference to this object so that method calls can be chained together.")
            .build();

        return CodeBlock.builder()
                        .add(javadocs, className)
                        .build();
    }

    public static CodeBlock waiterBuilderBuildJavadoc(ClassName className) {
        String javadocs = new DocumentationBuilder()
            .description("Builds an instance of {@link $T} based on the configurations supplied to this builder ")
            .returns("An initialized {@link $T}")
            .build();

        return CodeBlock.builder()
                        .add(javadocs, className, className)
                        .build();
    }

    public static CodeBlock waiterOperationWithOverrideConfigConsumerBuilder(ClassName clientClassName,
                                                                             ClassName requestClassName,
                                                                             Map.Entry<String, WaiterDefinition> waiterDefinition,
                                                                             OperationModel opModel) {
        String javadocs = new DocumentationBuilder().description("Polls {@link $T#$N} API until the desired condition "
                                                                 + "{@code $N} is met, "
                                                                 + "or until it is determined that the resource will never "
                                                                 + "enter into the desired state. \n "
                                                                 + "<p>This is a convenience method to create an instance of "
                                                                 + "the request builder and instance of the override config "
                                                                 + "builder")
                                                    .param(opModel.getInput().getVariableName(),
                                                           "The consumer that will configure the request to be used for polling")
                                                    .param("overrideConfig",
                                                           "The consumer that will configure the per request override "
                                                           + "configuration for waiters")
                                                    .returns("WaiterResponse containing either a response or an exception that "
                                                             + "has matched with the waiter success condition")
                                                    .build();
        return CodeBlock.builder()
                        .add(javadocs, clientClassName, opModel.getMethodName(), waiterDefinition.getKey())
                        .build();

    }

    public static CodeBlock waiterOperationWithOverrideConfig(ClassName clientClassName,
                                                              Map.Entry<String, WaiterDefinition> waiterDefinition,
                                                              OperationModel opModel) {
        String javadocs = new DocumentationBuilder().description("Polls {@link $T#$N} API until the desired condition "
                                                                 + "{@code $N} is met, "
                                                                 + "or until it is determined that the resource will never "
                                                                 + "enter into the desired state")
                                                    .param(opModel.getInput().getVariableName(), "The request to be"
                                                                                                 + " used"
                                                                                                 + " for polling")
                                                    .param("overrideConfig", "Per request "
                                                                             + "override configuration for waiters")
                                                    .returns("WaiterResponse containing either a response or an exception that "
                                                             + "has matched with the waiter success condition")
                                                    .build();
        return CodeBlock.builder()
                        .add(javadocs, clientClassName, opModel.getMethodName(), waiterDefinition.getKey())
                        .build();
    }

    public static CodeBlock waiterMethodInClient(ClassName waiterClassName) {
        String javadocs = new DocumentationBuilder()
            .description("Create an instance of {@link $T} using this client. \n"
                         + "<p>Waiters created via this method are managed by the SDK and resources will be released "
                         + "when the service client is closed.")
            .returns("an instance of {@link $T}")
            .build();

        return CodeBlock.builder()
                        .add(javadocs, waiterClassName, waiterClassName)
                        .build();
    }

}
