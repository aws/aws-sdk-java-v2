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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

public class SyncBatchManagerClassSpec extends BaseBatchManagerClassSpec {

    private final IntermediateModel model;
    private final PoetExtensions poetExtensions;
    private final ClassName className;

    public SyncBatchManagerClassSpec(IntermediateModel model) {
        super(model);
        this.model = model;
        this.poetExtensions = new PoetExtensions(model);
        this.className = poetExtensions.getBatchManagerSyncClass();
    }

    @Override
    public ClassName className() {
        return className;
    }

    @Override
    protected ClassName clientClassName() {
        return poetExtensions.getClientClass(model.getMetadata().getSyncInterface());
    }

    @Override
    protected ClassName interfaceClassName() {
        return poetExtensions.getBatchManagerSyncInterface();
    }

    @Override
    protected boolean isSync() {
        return true;
    }

    @Override
    protected void additionalTypeSpecModification(TypeSpec.Builder builder) {
        builder.addField(TypeName.BOOLEAN, "createdExecutor", PRIVATE)
               .addField(ClassName.get(Executor.class), "executor", PRIVATE, FINAL);
    }

    @Override
    protected void additionalConstructorInitialization(MethodSpec.Builder builder) {
        builder.addStatement("$T threadFactory = new $T().threadNamePrefix($S).build()",
                             ClassName.get(ThreadFactory.class), ClassName.get(ThreadFactoryBuilder.class), className)
               .beginControlFlow("if (builder.executor == null)")
               .addStatement("this.executor = createDefaultExecutor(threadFactory)")
               .addStatement("this.createdExecutor = true")
               .endControlFlow()
               .beginControlFlow("else")
               .addStatement("this.executor = builder.executor")
               .addStatement("this.createdExecutor = false")
               .endControlFlow();
    }

    @Override
    protected void additionalTestConstructorInitialization(MethodSpec.Builder builder) {
        builder.addParameter(ClassName.get(Executor.class), "executor")
               .addParameter(TypeName.BOOLEAN, "createdExecutor")
               .addStatement("this.executor = executor")
               .addStatement("this.createdExecutor = createdExecutor");
    }

    @Override
    protected void additionalBuilderTypeSpecModification(TypeSpec.Builder builder) {
        builder.addField(ClassName.get(Executor.class), "executor")
               .addMethod(MethodSpec.methodBuilder("executor")
                                    .addModifiers(PUBLIC)
                                    .addAnnotation(Override.class)
                                    .addParameter(ClassName.get(Executor.class), "executor")
                                    .returns(interfaceClassName().nestedClass("Builder"))
                                    .addStatement("this.executor = executor")
                                    .addStatement("return this", className())
                                    .build());
    }

    @Override
    protected void additionalCloseMethodModification(MethodSpec.Builder builder) {
        ClassName executorServiceClass = ClassName.get(ExecutorService.class);
        builder.beginControlFlow("if (createdExecutor && executor instanceof $T)", executorServiceClass)
               .addStatement("$T executorService = ($T) executor", executorServiceClass, executorServiceClass)
               .addStatement("executorService.shutdownNow()")
               .endControlFlow();
    }

    @Override
    protected void additionalExecutorInitialization(TypeSpec.Builder builder) {
        ClassName mathClass = ClassName.get(Math.class);
        ClassName threadPoolExecutorClass = ClassName.get(ThreadPoolExecutor.class);
        CodeBlock codeBlock = CodeBlock.builder()
                                       .addStatement("int processors = $T.getRuntime().availableProcessors()",
                                                     ClassName.get(Runtime.class))
                                       .addStatement("int corePoolSize = $T.max(8, processors)", mathClass)
                                       .addStatement("int maxPoolSize = $T.max(64, processors * 2)", mathClass)
                                       .addStatement("$T defaultExecutor = new $T(corePoolSize, maxPoolSize, 10, $T"
                                                     + ".SECONDS, new $T<>(1_000), threadFactory)",
                                                     threadPoolExecutorClass, threadPoolExecutorClass,
                                                     ClassName.get(TimeUnit.class), ClassName.get(LinkedBlockingQueue.class))
                                       .addStatement("// Allow idle core threads to time out\n"
                                                     + "defaultExecutor.allowCoreThreadTimeOut(true)")
                                       .addStatement("return defaultExecutor")
                                       .build();

        builder.addMethod(MethodSpec.methodBuilder("createDefaultExecutor")
                                    .addModifiers(PRIVATE)
                                    .addParameter(ClassName.get(ThreadFactory.class), "threadFactory")
                                    .addCode(codeBlock)
                                    .returns(ClassName.get(Executor.class))
                                    .build());
    }
}
