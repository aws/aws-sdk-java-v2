/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet.eventstream;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.eventstream.DefaultEventStreamResponseHandlerBuilder;
import software.amazon.awssdk.core.eventstream.EventStreamResponseHandlerFromBuilder;

public class EventStreamResponseHandlerBuilderSpec implements ClassSpec {

    private final OperationModel operation;
    private final PoetExtensions poetExt;
    private final EventStreamUtils eventStreamUtils;

    public EventStreamResponseHandlerBuilderSpec(GeneratorTaskParams params, OperationModel eventStreamOperation) {
        this.operation = eventStreamOperation;
        this.poetExt = params.getPoetExtensions();
        this.eventStreamUtils = new EventStreamUtils(eventStreamOperation);
    }

    @Override
    public TypeSpec poetSpec() {
        String apiName = eventStreamUtils.getApiName();
        ClassName responseClass = poetExt.getModelClass(operation.getOutputShape().getShapeName());
        ClassName eventStreamBaseClass = poetExt.getModelClass(
            eventStreamUtils.getEventStreamMember().getShapeName());
        ClassName responseHandler = poetExt.getModelClass(apiName + "ResponseHandler");
        ClassName responseHandlerBuilder = responseHandler.nestedClass("Builder");
        ParameterizedTypeName superBuilderClass =
            ParameterizedTypeName.get(ClassName.get(DefaultEventStreamResponseHandlerBuilder.class),
                                      responseClass, eventStreamBaseClass, responseHandlerBuilder);

        return PoetUtils.createClassBuilder(className())
                        .superclass(superBuilderClass)
                        .addSuperinterface(responseHandlerBuilder)
                        .addMethod(subscriberMethodSpec(responseHandler))
                        .addMethod(buildMethodSpec(responseHandler))
                        .addType(implInnerClass(responseHandler, responseClass, eventStreamBaseClass))
                        .build();
    }

    private TypeSpec implInnerClass(ClassName responseHandler, ClassName responseClass, ClassName eventStreamBaseClass) {
        ParameterizedTypeName superImplClass =
            ParameterizedTypeName.get(ClassName.get(EventStreamResponseHandlerFromBuilder.class),
                                      responseClass, eventStreamBaseClass);
        return PoetUtils.createClassBuilder(className().nestedClass("Impl"))
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .superclass(superImplClass)
                        .addSuperinterface(responseHandler)
                        .addMethod(MethodSpec.constructorBuilder()
                                             .addModifiers(Modifier.PRIVATE)
                                             .addParameter(ParameterSpec.builder(className(), "builder")
                                                                        .build())
                                             .addStatement("super(builder)")
                                             .build())
                        .build();
    }

    private MethodSpec subscriberMethodSpec(ClassName responseHandler) {
        return MethodSpec.methodBuilder("subscriber")
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Override.class)
                         .addParameter(ParameterSpec.builder(responseHandler.nestedClass("Visitor"), "visitor")
                                                    .build())
                         .addCode(CodeBlock.builder()
                                           .addStatement("subscriber(e -> e.accept(visitor))")
                                           .addStatement("return this")
                                           .build())
                         .returns(responseHandler.nestedClass("Builder"))
                         .build();
    }

    private MethodSpec buildMethodSpec(TypeName responseHandler) {
        return MethodSpec.methodBuilder("build")
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Override.class)
                         .addCode(CodeBlock.builder()
                                           .addStatement("return new Impl(this)")
                                           .build())
                         .returns(responseHandler)
                         .build();
    }

    @Override
    public ClassName className() {
        String className = String.format("Default%sResponseHandlerBuilder", eventStreamUtils.getApiName());
        return poetExt.getModelClass(className);
    }


}
