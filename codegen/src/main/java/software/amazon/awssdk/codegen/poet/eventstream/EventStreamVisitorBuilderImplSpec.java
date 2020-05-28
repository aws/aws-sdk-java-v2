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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;

/**
 * Generates the implementation for the builder of an event stream visitor.
 */
public class EventStreamVisitorBuilderImplSpec extends EventStreamVisitorBuilderInterfaceSpec {

    private final PoetExtensions poetExt;
    private final OperationModel opModel;
    private final ClassName visitorType;
    private final ClassName visitorBuilderType;
    private final ClassName eventStreamBaseClass;

    public EventStreamVisitorBuilderImplSpec(GeneratorTaskParams params, OperationModel operationModel) {
        super(params.getPoetExtensions(), operationModel);
        this.poetExt = params.getPoetExtensions();
        this.opModel = operationModel;
        this.visitorType = poetExt.eventStreamResponseHandlerVisitorType(opModel);
        this.visitorBuilderType = poetExt.eventStreamResponseHandlerVisitorBuilderType(opModel);
        this.eventStreamBaseClass = poetExt.getModelClassFromShape(
            EventStreamUtils.getEventStreamInResponse(operationModel.getOutputShape()));
    }

    @Override
    protected TypeSpec.Builder createTypeSpec() {
        return PoetUtils.createClassBuilder(className())
                        .addModifiers(Modifier.FINAL)
                        .addAnnotation(SdkInternalApi.class)
                        .addSuperinterface(visitorBuilderType)
                        .addField(FieldSpec.builder(consumerType(eventStreamBaseClass), "onDefault")
                                           .addModifiers(Modifier.PRIVATE)
                                           .build())
                        .addType(new VisitorFromBuilderImplSpec().poetSpec());
    }

    private class VisitorFromBuilderImplSpec extends EventStreamVisitorInterfaceSpec {

        private final MethodSpec.Builder constrBuilder;

        VisitorFromBuilderImplSpec() {
            super(poetExt, opModel);
            this.constrBuilder = MethodSpec.constructorBuilder()
                                           .addParameter(enclosingClassName(), "builder")
                                           .addStatement("this.onDefault = builder.onDefault != null ?\n"
                                                         + "builder.onDefault :\n"
                                                         + "$T.super::visitDefault", visitorType);
        }

        @Override
        protected TypeSpec.Builder createTypeSpec() {
            return PoetUtils.createClassBuilder(className())
                            .addModifiers(Modifier.STATIC)
                            .addField(consumerType(eventStreamBaseClass), "onDefault", Modifier.PRIVATE, Modifier.FINAL)
                            .addSuperinterface(visitorType);
        }

        @Override
        protected TypeSpec.Builder finalizeTypeSpec(TypeSpec.Builder builder) {
            return builder.addMethod(constrBuilder.build());
        }

        @Override
        protected MethodSpec.Builder applyVisitDefaultMethodSpecUpdates(MethodSpec.Builder builder) {
            return builder.addAnnotation(Override.class)
                          .addStatement("onDefault.accept(event)");
        }

        @Override
        protected MethodSpec.Builder applyVisitSubTypeMethodSpecUpdates(TypeSpec.Builder typeBuilder,
                                                                        MethodSpec.Builder methodBuilder,
                                                                        ShapeModel s) {
            ClassName eventSubType = poetExt.getModelClass(s.getShapeName());
            TypeName eventConsumerType = consumerType(eventSubType);
            FieldSpec consumerField = FieldSpec.builder(eventConsumerType, "on" + eventSubType.simpleName())
                                               .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                               .build();
            typeBuilder.addField(consumerField);

            constrBuilder.addStatement("this.$1L = builder.$1L != null ?\n"
                                       + "builder.$1L :\n"
                                       + "$2T.super::visit", consumerField.name, visitorType);
            return methodBuilder
                .addAnnotation(Override.class)
                .addStatement("$L.accept(event)", consumerField.name);
        }

        @Override
        public ClassName className() {
            return enclosingClassName().nestedClass("VisitorFromBuilder");
        }

        private ClassName enclosingClassName() {
            return EventStreamVisitorBuilderImplSpec.this.className();
        }
    }

    @Override
    protected MethodSpec.Builder applyOnSubTypeMethodSpecUpdates(TypeSpec.Builder typeBuilder,
                                                                 MethodSpec.Builder methodBuilder,
                                                                 ShapeModel eventSubTypeShape) {
        ClassName eventSubType = poetExt.getModelClass(eventSubTypeShape.getShapeName());
        TypeName eventConsumerType = consumerType(eventSubType);
        FieldSpec consumerField = FieldSpec.builder(eventConsumerType, "on" + eventSubType.simpleName())
                                           .addModifiers(Modifier.PRIVATE)
                                           .build();
        typeBuilder.addField(consumerField);
        return methodBuilder
            .addAnnotation(Override.class)
            .addStatement("this.$L = c", consumerField.name)
            .addStatement("return this");
    }

    @Override
    protected MethodSpec.Builder applyOnDefaultMethodSpecUpdates(MethodSpec.Builder builder) {
        return builder.addAnnotation(Override.class)
                      .addStatement("this.onDefault = c")
                      .addStatement("return this");

    }

    @Override
    protected MethodSpec.Builder applyBuildMethodSpecUpdates(MethodSpec.Builder builder) {
        return builder.addAnnotation(Override.class)
                      .addStatement("return new $T(this)",
                                    className().nestedClass("VisitorFromBuilder"));
    }

    @Override
    public ClassName className() {
        return poetExt.getModelClass(String.format("Default%sVisitorBuilder", poetExt.getApiName(opModel)));
    }

    private TypeName consumerType(ClassName paramType) {
        return ParameterizedTypeName.get(ClassName.get(Consumer.class), paramType);
    }

}
