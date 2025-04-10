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

package software.amazon.awssdk.codegen.poet.model;

import static javax.lang.model.element.Modifier.PUBLIC;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.eventstream.EventStreamUtils;

public final class EventModelSpec implements ClassSpec {
    private final MemberModel eventModel;
    private final ShapeModel eventStream;
    private final IntermediateModel intermediateModel;
    private final PoetExtension poetExtensions;
    private final ShapeModelSpec baseShapeModelSpec;
    private final TypeProvider typeProvider;
    private final EventStreamSpecHelper eventStreamSpecHelper;
    private final EventModelBuilderSpecs builderSpecs;

    public EventModelSpec(MemberModel eventModel, ShapeModel eventStream, IntermediateModel intermediateModel) {
        this.eventModel = eventModel;
        this.eventStream = eventStream;
        this.intermediateModel = intermediateModel;
        this.poetExtensions = new PoetExtension(intermediateModel);
        this.baseShapeModelSpec = new ShapeModelSpec(eventModel.getShape(), new TypeProvider(intermediateModel),
                poetExtensions, intermediateModel);
        this.typeProvider = new TypeProvider(intermediateModel);
        this.eventStreamSpecHelper = new EventStreamSpecHelper(eventStream, intermediateModel);
        this.builderSpecs = new EventModelBuilderSpecs(intermediateModel, eventModel, className(), typeProvider);
    }

    @Override
    public TypeSpec poetSpec() {
        ClassName eventStreamClassName = poetExtensions.getModelClassFromShape(eventStream);

        TypeSpec.Builder builder = TypeSpec.classBuilder(className())
                .superclass(baseShapeModelSpec.className())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(SdkInternalApi.class)
                .addAnnotation(PoetUtils.generatedAnnotation())
                .addJavadoc(classJavadoc())
                .addField(serialVersionUidField())
                .addMethod(constructor())
                .addMethod(toBuilderMethod())
                .addMethod(builderMethod())
                .addMethods(acceptMethods())
                .addMethod(sdkEventTypeMethodSpec())
                .addTypes(Arrays.asList(builderSpecs.builderInterface(), builderSpecs.beanStyleBuilder()));

        if (eventStreamSpecHelper.legacyEventGenerationMode() == CustomizationConfig.LegacyEventGenerationMode.DISABLED) {
            builder.addSuperinterface(eventStreamClassName);
        }
        return builder.build();
    }

    private CodeBlock classJavadoc() {
        return CodeBlock.builder()
                .add("A specialization of {@code $L} that represents the {@code $L$$$L} event. Do not use this class " +
                        "directly. Instead, use the static builder methods on {@link $L}.",
                        baseShapeModelSpec.className(), eventStream.getC2jName(),
                        eventModel.getC2jName(), poetExtensions.getModelClass(eventStream.getShapeName()))
                .build();

    }

    private FieldSpec serialVersionUidField() {
        return FieldSpec.builder(long.class, "serialVersionUID",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("1L")
                .build();
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                .addParameter(className().nestedClass("BuilderImpl"), "builderImpl")
                .addStatement("super($N)", "builderImpl")
                .build();
    }

    private MethodSpec toBuilderMethod() {
        return MethodSpec.methodBuilder("toBuilder")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(builderSpecs.builderInterfaceName())
                .addStatement("return new $T(this)", builderSpecs.builderImplName())
                .build();
    }

    private MethodSpec builderMethod() {
        return MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(builderSpecs.builderInterfaceName())
                .addStatement("return new $T()", builderSpecs.builderImplName())
                .build();
    }

    private List<MethodSpec> acceptMethods() {
        return operationsUsingEventOnOutput().stream()
                .map(o -> {
                    ClassName responseHandlerClass = poetExtensions.eventStreamResponseHandlerType(o);
                    return acceptMethodSpec(responseHandlerClass);
                })
                .collect(Collectors.toList());
    }

    @Override
    public ClassName className() {
        return eventStreamSpecHelper.eventClassName(eventModel);
    }

    private List<OperationModel> operationsUsingEventOnOutput() {
        Collection<OperationModel> opModels = EventStreamUtils.findOperationsWithEventStream(intermediateModel,
                eventStream);

        return opModels.stream()
                .filter(opModel -> EventStreamUtils.doesShapeContainsEventStream(opModel.getOutputShape(), eventStream))
                .collect(Collectors.toList());
    }


    private MethodSpec acceptMethodSpec(ClassName responseHandlerClass) {
        String visitMethodName = eventStreamSpecHelper.visitMethodName(eventModel);
        return MethodSpec.methodBuilder("accept")
                .addModifiers(PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(responseHandlerClass
                        .nestedClass("Visitor"), "visitor")
                .addStatement("visitor.$N(this)", visitMethodName)
                .build();
    }

    private MethodSpec sdkEventTypeMethodSpec() {
        ClassName eventTypeEnumClass = eventStreamSpecHelper.eventTypeEnumClassName();
        String eventTypeValue = eventStreamSpecHelper.eventTypeEnumValue(eventModel);
        return MethodSpec.methodBuilder("sdkEventType")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(eventTypeEnumClass)
                .addStatement("return $T.$N", eventTypeEnumClass, eventTypeValue)
                .build();
    }
}
