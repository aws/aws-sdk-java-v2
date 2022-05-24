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

package software.amazon.awssdk.codegen.poet.eventstream;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.docs.DocumentationBuilder;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.model.EventStreamSpecHelper;

/**
 * Spec for builder interface for visitor.
 */
public class EventStreamVisitorBuilderInterfaceSpec implements ClassSpec {
    private final PoetExtension poetExt;
    private final OperationModel operationModel;
    private final ShapeModel eventStreamShape;
    private final ClassName visitorBuilderType;
    private final EventStreamSpecHelper eventStreamSpecHelper;

    EventStreamVisitorBuilderInterfaceSpec(PoetExtension poetExt,
                                           IntermediateModel intermediateModel,
                                           OperationModel opModel) {
        this.poetExt = poetExt;
        this.operationModel = opModel;
        this.eventStreamShape = EventStreamUtils.getEventStreamInResponse(operationModel.getOutputShape());
        this.visitorBuilderType = poetExt.eventStreamResponseHandlerVisitorBuilderType(opModel);
        this.eventStreamSpecHelper = new EventStreamSpecHelper(eventStreamShape, intermediateModel);
    }

    @Override
    public final TypeSpec poetSpec() {
        TypeSpec.Builder typeBuilder = createTypeSpec()
            .addMethod(applyOnDefaultMethodSpecUpdates(createOnDefaultMethodSpec()).build())
            .addMethod(applyBuildMethodSpecUpdates(createBuildMethodSpec()).build());

        EventStreamUtils.getEvents(eventStreamShape)
                        .forEach(e -> typeBuilder.addMethod(
                            applyOnSubTypeMethodSpecUpdates(typeBuilder, createOnSubTypeMethodSpec(e), e)
                                .build()));

        return typeBuilder.build();
    }

    protected TypeSpec.Builder createTypeSpec() {
        String javadocs = "Builder for {@link $1T}. The {@link $1T} class may also be extended for a more "
                          + "traditional style but this builder allows for a more functional way of creating a visitor "
                          + "will callback methods.";
        return PoetUtils.createInterfaceBuilder(className())
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addJavadoc(javadocs, poetExt.eventStreamResponseHandlerVisitorType(operationModel));
    }

    @Override
    public ClassName className() {
        return visitorBuilderType;
    }

    private MethodSpec.Builder createOnDefaultMethodSpec() {
        ParameterizedTypeName eventConsumerType = ParameterizedTypeName.get(ClassName.get(Consumer.class),
                                                                            poetExt.getModelClassFromShape(eventStreamShape));
        return MethodSpec.methodBuilder("onDefault")
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(ParameterSpec.builder(eventConsumerType, "c").build())
                         .returns(visitorBuilderType);
    }

    protected MethodSpec.Builder applyOnDefaultMethodSpecUpdates(MethodSpec.Builder builder) {
        String javadocs = new DocumentationBuilder()
            .description("Callback to invoke when either an unknown event is visited or an unhandled event is visited.")
            .param("c", "Callback to process the event.")
            .returns("This builder for method chaining.")
            .build();
        return builder.addModifiers(Modifier.ABSTRACT)
                      .addJavadoc(javadocs);
    }

    private MethodSpec.Builder createBuildMethodSpec() {
        return MethodSpec.methodBuilder("build")
                         .addModifiers(Modifier.PUBLIC)
                         .returns(poetExt.eventStreamResponseHandlerVisitorType(operationModel));
    }

    protected MethodSpec.Builder applyBuildMethodSpecUpdates(MethodSpec.Builder builder) {
        return builder.addModifiers(Modifier.ABSTRACT)
                      .addJavadoc("@return Visitor implementation.");
    }

    protected MethodSpec.Builder applyOnSubTypeMethodSpecUpdates(TypeSpec.Builder typeBuilder,
                                                                 MethodSpec.Builder methodBuilder,
                                                                 MemberModel event) {
        ClassName eventSubType = poetExt.getModelClass(event.getShape().getShapeName());
        String javadocs = new DocumentationBuilder()
            .description("Callback to invoke when a {@link $T} is visited.")
            .param("c", "Callback to process the event.")
            .returns("This builder for method chaining.")
            .build();
        return methodBuilder.addModifiers(Modifier.ABSTRACT)
                            .addJavadoc(javadocs, eventSubType);
    }

    private MethodSpec.Builder createOnSubTypeMethodSpec(MemberModel event) {
        ClassName eventSubType = poetExt.getModelClass(event.getShape().getShapeName());
        ParameterizedTypeName eventConsumerType = ParameterizedTypeName.get(ClassName.get(Consumer.class), eventSubType);
        return MethodSpec.methodBuilder(eventStreamSpecHelper.eventConsumerName(event))
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(ParameterSpec.builder(eventConsumerType, "c").build())
                         .returns(visitorBuilderType);
    }
}
