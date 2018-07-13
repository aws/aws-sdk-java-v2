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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandler;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;

/**
 * Generates response handler interface for operations with an event stream response.
 */
public class EventStreamResponseHandlerSpec implements ClassSpec {

    private final PoetExtensions poetExt;
    private final EventStreamUtils eventStreamUtils;
    private final String apiName;
    private final ClassName responsePojoType;
    private final ClassName responseHandlerBuilderType;
    private final ClassName eventStreamBaseClass;

    public EventStreamResponseHandlerSpec(GeneratorTaskParams params, EventStreamUtils eventStreamUtils) {
        this.poetExt = params.getPoetExtensions();
        this.eventStreamUtils = eventStreamUtils;
        this.apiName = eventStreamUtils.getApiName();
        this.responsePojoType = eventStreamUtils.responsePojoType();
        this.responseHandlerBuilderType = eventStreamUtils.responseHandlerBuilderType();
        this.eventStreamBaseClass = eventStreamUtils.eventStreamBaseClass();
    }

    @Override
    public TypeSpec poetSpec() {
        ParameterizedTypeName superResponseHandlerInterface = ParameterizedTypeName.get(
            ClassName.get(EventStreamResponseHandler.class), responsePojoType, eventStreamBaseClass);
        return PoetUtils.createInterfaceBuilder(className())
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(SdkPublicApi.class)
                        .addSuperinterface(superResponseHandlerInterface)
                        .addJavadoc("Response handler for the $L API.", apiName)
                        .addMethod(builderMethodSpec())
                        .addType(new EventStreamResponseHandlerBuilderInterfaceSpec(eventStreamUtils).poetSpec())
                        .addType(new EventStreamVisitorInterfaceSpec(eventStreamUtils, poetExt).poetSpec())
                        .build();
    }

    @Override
    public ClassName className() {
        return eventStreamUtils.responseHandlerType();
    }

    private MethodSpec builderMethodSpec() {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .addJavadoc("Create a {@link $T}, used to create a {@link $T}.",
                                     responseHandlerBuilderType, className())
                         .addStatement("return new $T()",
                                       poetExt.getModelClass(String.format("Default%sResponseHandlerBuilder", apiName)))
                         .returns(responseHandlerBuilderType)
                         .build();
    }

}
