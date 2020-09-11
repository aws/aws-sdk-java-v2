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
import software.amazon.awssdk.core.waiters.PollingStrategy;

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
        String javadocs = new DocumentationBuilder().description("Polls {@link $T#$N} API until the desired condition "
                                                                 + "{@code $N} is met, "
                                                                 + "or until it is determined that the resource will never "
                                                                 + "enter into the desired state")
                                                    .param(operationModel.getInput().getVariableName(), "the request to be used"
                                                                                                        + " for polling")
                                                    .returns("WaiterResponse containing either a response or an exception that "
                                                             + "has matched with the waiter success condition")
                                                    .build();
        return CodeBlock.builder()
                        .add(javadocs, className, operationModel.getMethodName(), waiterDefinition.getKey())
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

    public static CodeBlock waiterBuilderPollingStrategy() {
        String javadocs = new DocumentationBuilder()
            .description("Defines a {@link $T} to use when polling a resource")
            .param("pollingStrategy", "the polling strategy to set")
            .returns("a reference to this object so that method calls can be chained together.")
            .build();

        return CodeBlock.builder()
                        .add(javadocs, ClassName.get(PollingStrategy.class))
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
            .description("Sets a custom {@link $T} that will be used to pool the resource \n "
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
}
