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

import static software.amazon.awssdk.codegen.internal.Constant.EVENT_PUBLISHER_PARAM_NAME;
import static software.amazon.awssdk.codegen.internal.Constant.EVENT_RESPONSE_HANDLER_PARAM_NAME;
import static software.amazon.awssdk.codegen.internal.Constant.SYNC_STREAMING_INPUT_PARAM;
import static software.amazon.awssdk.codegen.internal.Constant.SYNC_STREAMING_OUTPUT_PARAM;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.codegen.docs.SimpleMethodOverload;
import software.amazon.awssdk.codegen.model.config.customization.UtilitiesMethod;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.utils.PaginatorUtils;

public class DelegatingSyncClientClass extends SyncClientInterface {

    private final IntermediateModel model;
    private final ClassName className;
    private final PoetExtension poetExtensions;

    public DelegatingSyncClientClass(IntermediateModel model) {
        super(model);
        this.model = model;
        this.className = ClassName.get(model.getMetadata().getFullClientPackageName(),
                                       "Delegating" + model.getMetadata().getSyncInterface());
        this.poetExtensions = new PoetExtension(model);
    }

    @Override
    public TypeSpec poetSpec() {
        ClassName interfaceClass = poetExtensions.getClientClass(model.getMetadata().getSyncInterface());
        TypeSpec.Builder result = PoetUtils.createClassBuilder(className);

        result.addSuperinterface(interfaceClass)
              .addAnnotation(SdkProtectedApi.class)
              .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
              .addField(FieldSpec.builder(interfaceClass, "delegate")
                                 .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                                 .build())
              .addMethods(operations())
              .addMethod(closeMethod());

        result.addMethod(constructor(interfaceClass));

        if (model.getCustomizationConfig().getUtilitiesMethod() != null) {
            result.addMethod(utilitiesMethod());
        }

        if (model.hasWaiters()) {
            result.addMethod(waiterMethod());
        }

        return result.build();
    }

    private MethodSpec constructor(ClassName interfaceClass) {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(interfaceClass, "delegate")
                         .addStatement("this.delegate = delegate")
                         .build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    @Override
    protected MethodSpec.Builder simpleMethodModifier(MethodSpec.Builder builder) {
        return builder.addAnnotation(Override.class);
    }

    @Override
    protected MethodSpec.Builder operationBody(MethodSpec.Builder builder, OperationModel opModel) {
        builder.addModifiers(Modifier.PUBLIC)
               .addAnnotation(Override.class);

        if (opModel.hasStreamingInput() || opModel.hasStreamingOutput()) {
            String variableName = opModel.hasStreamingInput() ? SYNC_STREAMING_INPUT_PARAM : SYNC_STREAMING_OUTPUT_PARAM;
            return builder.addStatement("return delegate.$N($N, $N)",
                                        opModel.getMethodName(),
                                        opModel.getInput().getVariableName(),
                                        variableName);
        }

        if (opModel.hasEventStreamInput() && opModel.hasEventStreamOutput()) {
            return builder.addStatement("return delegate.$N($N, $N, $N)",
                                        opModel.getMethodName(),
                                        opModel.getInput().getVariableName(),
                                        EVENT_PUBLISHER_PARAM_NAME,
                                        SYNC_STREAMING_OUTPUT_PARAM);
        }

        if (opModel.hasEventStreamInput() || opModel.hasEventStreamOutput()) {
            String variableName = opModel.hasEventStreamInput() ? EVENT_PUBLISHER_PARAM_NAME : EVENT_RESPONSE_HANDLER_PARAM_NAME;
            return builder.addStatement("return delegate.$N($N, $N)",
                                        opModel.getMethodName(),
                                        opModel.getInput().getVariableName(),
                                        variableName);
        }

        return builder.addStatement("return delegate.$N($N)", opModel.getMethodName(), opModel.getInput().getVariableName());
    }

    @Override
    protected MethodSpec.Builder paginatedMethodBody(MethodSpec.Builder builder, OperationModel opModel) {
        String methodName = PaginatorUtils.getPaginatedMethodName(opModel.getMethodName());
        return builder.addModifiers(Modifier.PUBLIC)
                      .addAnnotation(Override.class)
                      .addStatement("return delegate.$N($N)", methodName, opModel.getInput().getVariableName());
    }

    @Override
    protected MethodSpec.Builder utilitiesOperationBody(MethodSpec.Builder builder) {
        return builder.addAnnotation(Override.class).addStatement("return delegate.$N()", UtilitiesMethod.METHOD_NAME);
    }

    @Override
    protected MethodSpec.Builder waiterOperationBody(MethodSpec.Builder builder) {
        return builder.addAnnotation(Override.class).addStatement("return delegate.waiter()");
    }

    @Override
    protected void operationAddConsumer(List<MethodSpec> specs, OperationModel opModel) {
    }

    @Override
    protected void paginatedMethodAddConsumer(List<MethodSpec> specs, MethodSpec spec, OperationModel opModel) {
    }

    @Override
    protected void streamingSimpleMethodsAddConsumer(List<MethodSpec> specs, MethodSpec spec, SimpleMethodOverload overload,
                                                     OperationModel opModel) {
    }

    private MethodSpec closeMethod() {
        return MethodSpec.methodBuilder("close")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addStatement("delegate.close()")
                         .build();
    }
}
