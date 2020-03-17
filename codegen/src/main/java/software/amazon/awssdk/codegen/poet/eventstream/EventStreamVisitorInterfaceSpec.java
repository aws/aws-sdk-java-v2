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
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.docs.DocumentationBuilder;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;

/**
 * Spec for generated visitor interface.
 */
class EventStreamVisitorInterfaceSpec implements ClassSpec {

    private final PoetExtensions poetExt;
    private final OperationModel operationModel;
    private final ShapeModel eventStreamShape;
    private final ClassName eventStreamBaseClass;

    EventStreamVisitorInterfaceSpec(PoetExtensions poetExt, OperationModel operationModel) {
        this.poetExt = poetExt;
        this.operationModel = operationModel;
        this.eventStreamShape = EventStreamUtils.getEventStreamInResponse(operationModel.getOutputShape());
        this.eventStreamBaseClass = poetExt.getModelClassFromShape(eventStreamShape);
    }

    @Override
    public final TypeSpec poetSpec() {
        TypeSpec.Builder typeBuilder = createTypeSpec()
            .addMethod(applyVisitDefaultMethodSpecUpdates(createVisitDefaultMethodSpec()).build());

        EventStreamUtils.getEvents(eventStreamShape)
                        .forEach(s -> typeBuilder.addMethod(
                            applyVisitSubTypeMethodSpecUpdates(typeBuilder, createVisitSubTypeMethodSpec(s), s)
                                .build()));


        return finalizeTypeSpec(typeBuilder).build();
    }

    @Override
    public ClassName className() {
        return poetExt.eventStreamResponseHandlerVisitorType(operationModel);
    }

    protected TypeSpec.Builder createTypeSpec() {
        return PoetUtils.createInterfaceBuilder(className())
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addJavadoc("Visitor for subtypes of {@link $T}.", eventStreamBaseClass)
                        .addMethod(createBuilderMethodSpec())
                        .addType(new EventStreamVisitorBuilderInterfaceSpec(poetExt, operationModel).poetSpec());
    }

    protected TypeSpec.Builder finalizeTypeSpec(TypeSpec.Builder builder) {
        return builder;
    }

    private MethodSpec.Builder createVisitDefaultMethodSpec() {
        return MethodSpec.methodBuilder("visitDefault")
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(ParameterSpec.builder(eventStreamBaseClass, "event").build());
    }

    protected MethodSpec.Builder applyVisitDefaultMethodSpecUpdates(MethodSpec.Builder builder) {
        String javadocs = new DocumentationBuilder()
            .description("A required \"else\" or \"default\" block, invoked when no other more-specific \"visit\" method is "
                         + "appropriate. This is invoked under two circumstances:\n"
                         + "<ol>\n"
                         + "<li>The event encountered is newer than the current version of the SDK, so no other "
                         + "more-specific \"visit\" method could be called. In this case, the provided event will be a "
                         + "generic {@link $1T}. These events can be processed by upgrading the SDK.</li>\n"
                         + "<li>The event is known by the SDK, but the \"visit\" was not overridden above. In this case, the "
                         + "provided event will be a specific type of {@link $1T}.</li>\n"
                         + "</ol>")
            .param("event", "The event that was not handled by a more-specific \"visit\" method.")
            .build();
        return builder.addModifiers(Modifier.DEFAULT)
                      .addJavadoc(javadocs, eventStreamBaseClass);
    }

    private MethodSpec.Builder createVisitSubTypeMethodSpec(ShapeModel s) {
        ClassName eventSubType = poetExt.getModelClass(s.getShapeName());
        return MethodSpec.methodBuilder("visit")
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(ParameterSpec.builder(eventSubType, "event").build());
    }

    protected MethodSpec.Builder applyVisitSubTypeMethodSpecUpdates(TypeSpec.Builder typeBuilder,
                                                                    MethodSpec.Builder methodBuilder,
                                                                    ShapeModel eventShape) {
        ClassName eventSubType = poetExt.getModelClass(eventShape.getShapeName());
        String javadocs = new DocumentationBuilder()
            .description("Invoked when a {@link $T} is encountered. If this is not overridden, the event will "
                         + "be given to {@link #visitDefault($T)}.")
            .param("event", "Event being visited")
            .build();
        return methodBuilder.addModifiers(Modifier.DEFAULT)
                            .addStatement("visitDefault(event)")
                            .addJavadoc(javadocs, eventSubType, eventStreamBaseClass);
    }

    private MethodSpec createBuilderMethodSpec() {
        ClassName visitorBuilderType = poetExt.eventStreamResponseHandlerVisitorBuilderType(operationModel);
        return MethodSpec.methodBuilder("builder")
                         .addJavadoc("@return A new {@link Builder}.", visitorBuilderType)
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .returns(visitorBuilderType)
                         .addStatement("return new Default$LVisitorBuilder()", poetExt.getApiName(operationModel))
                         .build();
    }
}
