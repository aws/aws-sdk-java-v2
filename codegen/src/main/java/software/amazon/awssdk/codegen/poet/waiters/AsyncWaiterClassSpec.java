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

package software.amazon.awssdk.codegen.poet.waiters;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.core.internal.waiters.WaiterAttribute;
import software.amazon.awssdk.core.waiters.AsyncWaiter;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

public class AsyncWaiterClassSpec extends BaseWaiterClassSpec {

    private final PoetExtensions poetExtensions;
    private final ClassName className;
    private final IntermediateModel model;
    private final String modelPackage;

    public AsyncWaiterClassSpec(IntermediateModel model) {
        super(model, ClassName.get(AsyncWaiter.class));
        this.model = model;
        this.modelPackage = model.getMetadata().getFullModelPackageName();
        this.poetExtensions = new PoetExtensions(model);
        this.className = poetExtensions.getAsyncWaiterClass();
    }

    @Override
    public ClassName className() {
        return className;
    }

    @Override
    protected ClassName clientClassName() {
        return poetExtensions.getClientClass(model.getMetadata().getAsyncInterface());
    }

    @Override
    protected ParameterizedTypeName getWaiterResponseType(OperationModel opModel) {
        ClassName pojoResponse = ClassName.get(modelPackage, opModel.getReturnType().getReturnType());
        ParameterizedTypeName waiterResponse = ParameterizedTypeName.get(ClassName.get(WaiterResponse.class), pojoResponse);

        return ParameterizedTypeName.get(ClassName.get(CompletableFuture.class),
                                         waiterResponse);
    }

    @Override
    protected ClassName interfaceClassName() {
        return poetExtensions.getAsyncWaiterInterface();
    }

    @Override
    protected Optional<String> additionalWaiterConfig() {
        return Optional.of(".scheduledExecutorService(executorService)");
    }

    @Override
    protected void additionalConstructorInitialization(MethodSpec.Builder method) {
        method.beginControlFlow("if (builder.executorService == null)")
              .addStatement("this.executorService = $T.newScheduledThreadPool(1, new $T().threadNamePrefix"
                            + "($S).build())",
                            Executors.class,
                            ThreadFactoryBuilder.class,
                            "waiters-ScheduledExecutor")
              .addStatement("attributeMapBuilder.put(SCHEDULED_EXECUTOR_SERVICE_ATTRIBUTE, this.executorService)")
              .endControlFlow();

        method.beginControlFlow("else")
              .addStatement("this.executorService = builder.executorService")
              .endControlFlow();
    }

    @Override
    protected void additionalTypeSpecModification(TypeSpec.Builder type) {
        type.addField(FieldSpec.builder(ParameterizedTypeName.get(WaiterAttribute.class, ScheduledExecutorService.class),
                                        "SCHEDULED_EXECUTOR_SERVICE_ATTRIBUTE", PRIVATE, STATIC, FINAL)
                               .initializer("new $T<>($T.class)", WaiterAttribute.class, ScheduledExecutorService.class)
                               .build());
        type.addField(FieldSpec.builder(ScheduledExecutorService.class, "executorService")
                               .addModifiers(PRIVATE, FINAL)
                               .build());
    }

    @Override
    protected void additionalBuilderTypeSpecModification(TypeSpec.Builder type) {
        type.addField(ClassName.get(ScheduledExecutorService.class), "executorService", PRIVATE);
        type.addMethod(MethodSpec.methodBuilder("scheduledExecutorService")
                                 .addModifiers(Modifier.PUBLIC)
                                 .addAnnotation(Override.class)
                                 .addParameter(ClassName.get(ScheduledExecutorService.class), "executorService")
                                 .addStatement("this.executorService = executorService")
                                 .addStatement("return this")
                                 .returns(interfaceClassName().nestedClass("Builder"))
                                 .build());
    }
}