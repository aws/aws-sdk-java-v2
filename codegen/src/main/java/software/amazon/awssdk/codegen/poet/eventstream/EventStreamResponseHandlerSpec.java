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
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.lang.model.element.Modifier;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.codegen.docs.DocumentationBuilder;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.eventstream.EventStreamResponseHandler;

public class EventStreamResponseHandlerSpec implements ClassSpec {

    private final PoetExtensions poetExt;
    private final EventStreamUtils eventStreamUtils;
    private final String apiName;
    private final ClassName responseClass;
    private final ClassName eventStreamBaseClass;

    public EventStreamResponseHandlerSpec(GeneratorTaskParams params, OperationModel operation) {
        this.poetExt = params.getPoetExtensions();
        this.eventStreamUtils = new EventStreamUtils(operation);
        this.apiName = eventStreamUtils.getApiName();
        this.responseClass = poetExt.getModelClass(operation.getOutputShape().getShapeName());
        this.eventStreamBaseClass = poetExt.getModelClass(
            eventStreamUtils.getEventStreamMember().getShapeName());
    }

    @Override
    public TypeSpec poetSpec() {
        ParameterizedTypeName superResponseHandlerInterface = ParameterizedTypeName.get(
            ClassName.get(EventStreamResponseHandler.class), responseClass, eventStreamBaseClass);
        ClassName nestedBuilderInterface = className().nestedClass("Builder");
        return PoetUtils.createInterfaceBuilder(className())
                        .addModifiers(Modifier.PUBLIC)
                        .addSuperinterface(superResponseHandlerInterface)
                        .addJavadoc("Response handler for the $L API.", apiName)
                        .addMethod(builderMethodSpec(className(), nestedBuilderInterface))
                        .addType(new BuilderSpec().poetSpec())
                        .addType(new VisitorSpec().poetSpec())
                        .build();
    }

    @Override
    public ClassName className() {
        return poetExt.getModelClass(apiName + "ResponseHandler");
    }

    private MethodSpec builderMethodSpec(ClassName responseHandler, ClassName nestedBuilderInterface) {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .addJavadoc("Create a {@link $T}, used to create a {@link $T}.",
                                     nestedBuilderInterface, responseHandler)
                         .addStatement("return new $T()",
                                       poetExt.getModelClass(String.format("Default%sResponseHandlerBuilder", apiName)))
                         .returns(nestedBuilderInterface)
                         .build();
    }

    /**
     * Spec for generated response handler builder
     */
    private class BuilderSpec implements ClassSpec {

        @Override
        public TypeSpec poetSpec() {
            ClassName builderInterface = className();
            ParameterizedTypeName superBuilderInterface = ParameterizedTypeName.get(
                ClassName.get(EventStreamResponseHandler.class).nestedClass("Builder"),
                responseClass, eventStreamBaseClass, builderInterface);
            return PoetUtils.createInterfaceBuilder(builderInterface)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .addJavadoc("Builder for {@link $1T}. This can be used to create the {@link $1T} in a more "
                                        + "functional way, you may also directly implement the {@link $1T} interface if "
                                        + "preferred.",
                                        enclosingClass())
                            .addMethod(subscriberMethodSpec())
                            .addMethod(buildMethodSpec(enclosingClass()))
                            .addSuperinterface(superBuilderInterface)
                            .build();
        }

        private MethodSpec subscriberMethodSpec() {
            String javadocs = new DocumentationBuilder()
                .description("Sets the subscriber to the {@link $T} of events. The given "
                             + "{@link $T} will be called for each event received by the "
                             + "publisher. Events are requested sequentially after each event is "
                             + "processed. If you need more control over the backpressure strategy "
                             + "consider using {@link #subscriber($T)} instead.")
                .param("visitor", "Visitor that will be invoked for each incoming event.")
                .returns("This builder for method chaining")
                .build();
            ClassName visitorInterface = enclosingClass().nestedClass("Visitor");
            return MethodSpec.methodBuilder("subscriber")
                             .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                             .addJavadoc(javadocs,
                                         Publisher.class, visitorInterface, Supplier.class)
                             .addParameter(ParameterSpec.builder(visitorInterface, "visitor")
                                                        .build())
                             .returns(className())
                             .build();
        }

        private MethodSpec buildMethodSpec(ClassName responseHandler) {
            return MethodSpec.methodBuilder("build")
                             .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                             .addJavadoc("@return A {@link $T} implementation that can be used in the $L API call.",
                                         responseHandler, apiName)
                             .returns(responseHandler)
                             .build();
        }

        @Override
        public ClassName className() {
            return enclosingClass().nestedClass("Builder");
        }

        private ClassName enclosingClass() {
            return EventStreamResponseHandlerSpec.this.className();
        }
    }

    /**
     * Spec for generated visitor interface.
     */
    private class VisitorSpec implements ClassSpec {

        @Override
        public TypeSpec poetSpec() {
            TypeSpec.Builder builder = PoetUtils.createInterfaceBuilder(className())
                                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                .addJavadoc("Visitor for subtypes of {@link $T}.", eventStreamBaseClass)
                                                .addMethod(visitDefault())
                                                .addMethod(visitorBuilderMethodSpec(className()))
                                                .addType(new VisitorBuilderSpec().poetSpec());

            eventStreamUtils.getEventSubTypes()
                            .forEach(s -> builder.addMethod(visitSubType(s)));


            return builder.build();
        }

        @Override
        public ClassName className() {
            return EventStreamResponseHandlerSpec.this.className().nestedClass("Visitor");
        }

        private MethodSpec visitDefault() {
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
            return MethodSpec.methodBuilder("visitDefault")
                             .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                             .addJavadoc(javadocs, eventStreamBaseClass)
                             .addParameter(ParameterSpec.builder(eventStreamBaseClass, "event").build())
                             .build();
        }

        private MethodSpec visitSubType(ShapeModel s) {
            String javadocs = new DocumentationBuilder()
                .description("Invoked when a {@link $T} is encountered. If this is not overridden, the event will "
                             + "be given to {@link #visitDefault($T)}.")
                .param("event", "Event being visited")
                .build();
            ClassName eventSubType = poetExt.getModelClass(s.getShapeName());
            return MethodSpec.methodBuilder("visit")
                             .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                             .addJavadoc(javadocs, eventSubType, eventStreamBaseClass)
                             .addParameter(ParameterSpec.builder(eventSubType, "event").build())
                             .addStatement("visitDefault(event)")
                             .build();
        }

        private MethodSpec visitorBuilderMethodSpec(ClassName visitorInterface) {
            ClassName builderInterfaceType = visitorInterface.nestedClass("Builder");
            return MethodSpec.methodBuilder("builder")
                             .addJavadoc("@return A new {@link Builder}.", builderInterfaceType)
                             .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                             .returns(builderInterfaceType)
                             .addStatement("return new Default$LVisitorBuilder()", apiName)
                             .build();
        }

        /**
         * Spec for builder interface for visitor.
         */
        private class VisitorBuilderSpec implements ClassSpec {

            @Override
            public TypeSpec poetSpec() {
                String javadocs = "Builder for {@link $1T}. The {@link $1T} class may also be extended for a more "
                                  + "traditional style but this builder allows for a more functional way of creating a visitor "
                                  + "will callback methods.";
                TypeSpec.Builder builder = PoetUtils.createInterfaceBuilder(className())
                                                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                    .addJavadoc(javadocs, enclosingClass())
                                                    .addMethod(visitorBuilderOnDefaultMethodSpec())
                                                    .addMethod(visitorBuilderBuildMethodSpec());

                eventStreamUtils.getEventSubTypes()
                                .forEach(s -> builder.addMethod(visitorBuilderOnSubTypeMethodSpec(s)));

                return builder.build();
            }

            @Override
            public ClassName className() {
                return enclosingClass().nestedClass("Builder");
            }

            private ClassName enclosingClass() {
                return VisitorSpec.this.className();
            }

            private MethodSpec visitorBuilderOnDefaultMethodSpec() {
                String javadocs = new DocumentationBuilder()
                    .description("Callback to invoke when either an unknown event is visited or an unhandled event is visited.")
                    .param("c", "Callback to process the event.")
                    .returns("This builder for method chaining.")
                    .build();
                ParameterizedTypeName eventConsumerType = ParameterizedTypeName.get(ClassName.get(Consumer.class),
                                                                                    eventStreamBaseClass);
                return MethodSpec.methodBuilder("onDefault")
                                 .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                 .addJavadoc(javadocs)
                                 .addParameter(ParameterSpec.builder(eventConsumerType, "c").build())
                                 .returns(className())
                                 .build();
            }

            private MethodSpec visitorBuilderBuildMethodSpec() {
                return MethodSpec.methodBuilder("build")
                                 .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                 .addJavadoc("@return Visitor implementation.")
                                 .returns(enclosingClass())
                                 .build();
            }

            private MethodSpec visitorBuilderOnSubTypeMethodSpec(ShapeModel eventSubTypeShape) {
                ClassName eventSubType = poetExt.getModelClass(eventSubTypeShape.getShapeName());
                String javadocs = new DocumentationBuilder()
                    .description("Callback to invoke when a {@link $T} is visited.")
                    .param("c", "Callback to process the event.")
                    .returns("This builder for method chaining.")
                    .build();
                ParameterizedTypeName eventConsumerType = ParameterizedTypeName.get(ClassName.get(Consumer.class), eventSubType);
                return MethodSpec.methodBuilder("on" + eventSubType.simpleName())
                                 .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                 .addJavadoc(javadocs, eventSubType)
                                 .addParameter(ParameterSpec.builder(eventConsumerType, "c").build())
                                 .returns(className())
                                 .build();
            }

        }
    }

}
