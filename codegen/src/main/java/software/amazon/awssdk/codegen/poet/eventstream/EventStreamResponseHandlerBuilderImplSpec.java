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
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.eventstream.DefaultEventStreamResponseHandlerBuilder;
import software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandlerFromBuilder;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;

/**
 * Generates implementation class for the event stream response handler builder.
 */
public class EventStreamResponseHandlerBuilderImplSpec extends EventStreamResponseHandlerBuilderInterfaceSpec {

    private final PoetExtensions poetExt;
    private final ClassName responseHandlerType;
    private final ClassName responseHandlerBuilderType;
    private final ClassName eventStreamBaseClass;

    public EventStreamResponseHandlerBuilderImplSpec(GeneratorTaskParams params, EventStreamUtils eventStreamUtils) {
        super(eventStreamUtils);
        this.poetExt = params.getPoetExtensions();
        this.responseHandlerType = eventStreamUtils.responseHandlerType();
        this.responseHandlerBuilderType = eventStreamUtils.responseHandlerBuilderType();
        this.eventStreamBaseClass = eventStreamUtils.eventStreamBaseClass();
    }

    @Override
    protected TypeSpec.Builder createTypeSpecBuilder() {
        ClassName responsePojoType = eventStreamUtils.responsePojoType();
        ParameterizedTypeName superBuilderClass =
            ParameterizedTypeName.get(ClassName.get(DefaultEventStreamResponseHandlerBuilder.class),
                                      responsePojoType, eventStreamBaseClass, responseHandlerBuilderType);
        return PoetUtils.createClassBuilder(className())
                        .addModifiers(Modifier.FINAL)
                        .addAnnotation(SdkInternalApi.class)
                        .superclass(superBuilderClass)
                        .addSuperinterface(super.className())
                        .addType(implInnerClass(responsePojoType));
    }

    @Override
    protected MethodSpec.Builder applySubscriberMethodSpecUpdates(MethodSpec.Builder builder) {
        return builder
            .addAnnotation(Override.class)
            .addCode(CodeBlock.builder()
                              .addStatement("subscriber(e -> e.accept(visitor))")
                              .addStatement("return this")
                              .build());
    }

    @Override
    protected MethodSpec.Builder applyBuildMethodSpecUpdates(MethodSpec.Builder builder) {
        return builder
            .addAnnotation(Override.class)
            .addCode(CodeBlock.builder()
                              .addStatement("return new Impl(this)")
                              .build());
    }

    @Override
    public ClassName className() {
        String className = String.format("Default%sResponseHandlerBuilder", eventStreamUtils.getApiName());
        return poetExt.getModelClass(className);
    }

    private TypeSpec implInnerClass(ClassName responseClass) {
        ParameterizedTypeName superImplClass =
            ParameterizedTypeName.get(ClassName.get(EventStreamResponseHandlerFromBuilder.class),
                                      responseClass, eventStreamBaseClass);
        return PoetUtils.createClassBuilder(className().nestedClass("Impl"))
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .superclass(superImplClass)
                        .addSuperinterface(responseHandlerType)
                        .addMethod(MethodSpec.constructorBuilder()
                                             .addModifiers(Modifier.PRIVATE)
                                             .addParameter(ParameterSpec.builder(className(), "builder")
                                                                        .build())
                                             .addStatement("super(builder)")
                                             .build())
                        .build();
    }

}
