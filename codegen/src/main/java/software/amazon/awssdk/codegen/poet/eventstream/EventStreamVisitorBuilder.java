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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public class EventStreamVisitorBuilder implements ClassSpec {

    private final PoetExtensions poetExt;
    private final EventStreamUtils eventStreamUtils;
    private final OperationModel operation;

    public EventStreamVisitorBuilder(GeneratorTaskParams params, OperationModel operationModel) {
        this.poetExt = params.getPoetExtensions();
        this.operation = operationModel;
        this.eventStreamUtils = new EventStreamUtils(operationModel);

    }

    @Override
    public TypeSpec poetSpec() {
        ClassName eventStreamBaseClass = poetExt.getModelClass(
            eventStreamUtils.getEventStreamMember().getShapeName());
        ClassName responseHandler = poetExt.getModelClass(eventStreamUtils.getApiName() + "ResponseHandler");
        ClassName visitorInterface = responseHandler.nestedClass("Visitor");
        ClassName visitorBuilderInterface = visitorInterface.nestedClass("Builder");
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className())
                                            .addSuperinterface(visitorBuilderInterface)
                                            .addField(FieldSpec.builder(consumerType(eventStreamBaseClass), "onDefault")
                                                               .addModifiers(Modifier.PRIVATE)
                                                               .build())
                                            .addMethod(onDefaultMethodSpec(eventStreamBaseClass, visitorBuilderInterface))
                                            .addMethod(buildMethodSpec(visitorInterface))
                                            .addType(visitorFromBuilderTypeSpec(visitorInterface, eventStreamBaseClass));

        eventStreamUtils.getEventSubTypes().forEach(s -> {
            ClassName eventSubType = poetExt.getModelClass(s.getShapeName());
            TypeName eventConsumerType = consumerType(eventSubType);
            FieldSpec consumerField = FieldSpec.builder(eventConsumerType, "on" + eventSubType.simpleName())
                                               .addModifiers(Modifier.PRIVATE)
                                               .build();
            builder.addField(consumerField);
            builder.addMethod(MethodSpec.methodBuilder("on" + eventSubType.simpleName())
                                        .addAnnotation(Override.class)
                                        .addModifiers(Modifier.PUBLIC)
                                        .addParameter(eventConsumerType, "c")
                                        .addStatement("this.$L = c", consumerField.name)
                                        .addStatement("return this")
                                        .returns(visitorBuilderInterface)
                                        .build());
        });

        return builder.build();
    }

    private TypeSpec visitorFromBuilderTypeSpec(ClassName visitorInterface,
                                                ClassName eventStreamBaseClass) {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className().nestedClass("VisitorFromBuilder"));

        MethodSpec.Builder constrBuilder = MethodSpec.constructorBuilder()
                                                     .addParameter(className(), "builder");
        eventStreamUtils.getEventSubTypes().forEach(s -> {
            ClassName eventSubType = poetExt.getModelClass(s.getShapeName());
            TypeName eventConsumerType = consumerType(eventSubType);
            FieldSpec consumerField = FieldSpec.builder(eventConsumerType, "on" + eventSubType.simpleName())
                                               .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                               .build();
            builder.addField(consumerField);
            builder.addMethod(MethodSpec.methodBuilder("visit")
                                        .addAnnotation(Override.class)
                                        .addModifiers(Modifier.PUBLIC)
                                        .addParameter(eventSubType, "event")
                                        .addStatement("$L.accept(event)", consumerField.name)
                                        .build());

            constrBuilder.addStatement("this.$1L = builder.$1L != null ?\n"
                                       + "builder.$1L :\n"
                                       + "$2T.super::visit", consumerField.name, visitorInterface);
        });

        constrBuilder.addStatement("this.onDefault = builder.onDefault != null ?\n"
                                   + "builder.onDefault :\n"
                                   + "$T.super::visitDefault", visitorInterface);

        return builder
            .addField(consumerType(eventStreamBaseClass), "onDefault", Modifier.PRIVATE, Modifier.FINAL)
            .addSuperinterface(visitorInterface)
            .addMethod(constrBuilder.build())
            .addMethod(visitDefaultMethodSpec(eventStreamBaseClass))
            .build();
    }

    private MethodSpec visitDefaultMethodSpec(ClassName eventStreamBaseClass) {
        return MethodSpec.methodBuilder("visitDefault")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(eventStreamBaseClass, "event")
                         .addStatement("onDefault.accept(event)")
                         .build();
    }

    private MethodSpec buildMethodSpec(ClassName visitorInterface) {
        return MethodSpec.methodBuilder("build")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addStatement("return new $T(this)",
                                       className().nestedClass("VisitorFromBuilder"))
                         .returns(visitorInterface)
                         .build();
    }

    private MethodSpec onDefaultMethodSpec(ClassName eventStreamBaseClass, ClassName visitorBuilderInterface) {
        return MethodSpec.methodBuilder("onDefault")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(consumerType(eventStreamBaseClass), "c")
                         .addStatement("this.onDefault = c")
                         .addStatement("return this")
                         .returns(visitorBuilderInterface)
                         .build();
    }

    private TypeName consumerType(ClassName paramType) {
        return ParameterizedTypeName.get(ClassName.get(Consumer.class), paramType);
    }

    @Override
    public ClassName className() {
        return poetExt.getModelClass(String.format("Default%sVisitorBuilder", eventStreamUtils.getApiName()));
    }

}
