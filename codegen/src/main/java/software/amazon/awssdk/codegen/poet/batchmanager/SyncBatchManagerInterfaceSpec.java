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

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static software.amazon.awssdk.codegen.docs.BatchManagerDocs.batchManagerBuilderExecutorJavadoc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.concurrent.Executor;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;

public class SyncBatchManagerInterfaceSpec extends BaseBatchManagerInterfaceSpec {

    private final IntermediateModel model;
    private final PoetExtensions poetExtensions;
    private final ClassName className;

    public SyncBatchManagerInterfaceSpec(IntermediateModel model) {
        super(model);
        this.model = model;
        this.poetExtensions = new PoetExtensions(model);
        this.className = poetExtensions.getBatchManagerSyncInterface();
    }

    @Override
    public ClassName className() {
        return className;
    }

    @Override
    public ClassName defaultBatchManagerName() {
        return poetExtensions.getBatchManagerSyncClass();
    }

    @Override
    public ClassName clientClassName() {
        return poetExtensions.getClientClass(model.getMetadata().getSyncInterface());
    }

    @Override
    public boolean isSync() {
        return true;
    }

    @Override
    protected void additionalBuilderTypeSpecModification(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder("executor")
                                 .addModifiers(PUBLIC, ABSTRACT)
                                 .addParameter(ClassName.get(Executor.class), "executor")
                                 .addJavadoc(batchManagerBuilderExecutorJavadoc(className.simpleName()))
                                 .returns(className().nestedClass("Builder"))
                                 .build());
    }
}
