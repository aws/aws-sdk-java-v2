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

import static software.amazon.awssdk.codegen.internal.Constant.SYNC_STREAMING_INPUT_PARAM;
import static software.amazon.awssdk.codegen.internal.Constant.SYNC_STREAMING_OUTPUT_PARAM;
import static software.amazon.awssdk.codegen.internal.Constant.EVENT_PUBLISHER_PARAM_NAME;
import static software.amazon.awssdk.codegen.internal.Constant.EVENT_RESPONSE_HANDLER_PARAM_NAME;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
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
        this.className = ClassName.get(model.getMetadata().getFullInternalPackageName(),
                                       "Delegating" + model.getMetadata().getSyncInterface());
        this.poetExtensions = new PoetExtension(model);
    }

    @Override
    public TypeSpec poetSpec() {
        ClassName interfaceClass = poetExtensions.getClientClass(model.getMetadata().getSyncInterface());
        TypeSpec.Builder result = PoetUtils.createClassBuilder(className);

        result.addSuperinterface(interfaceClass)
              .addAnnotation(SdkInternalApi.class)
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


    private MethodSpec closeMethod() {
        return MethodSpec.methodBuilder("close")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addStatement("delegate.close()")
                         .build();
    }

}
