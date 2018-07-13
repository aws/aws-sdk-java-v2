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
import java.util.function.Supplier;
import javax.lang.model.element.Modifier;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandler;
import software.amazon.awssdk.codegen.docs.DocumentationBuilder;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;

/**
 * Spec for generated response handler builder
 */
public class EventStreamResponseHandlerBuilderInterfaceSpec implements ClassSpec {

    final EventStreamUtils eventStreamUtils;
    private final ClassName responsePojoType;
    private final ClassName responseHandlerType;

    EventStreamResponseHandlerBuilderInterfaceSpec(EventStreamUtils eventStreamUtils) {
        this.eventStreamUtils = eventStreamUtils;
        this.responsePojoType = eventStreamUtils.responsePojoType();
        this.responseHandlerType = eventStreamUtils.responseHandlerType();
    }

    @Override
    public final TypeSpec poetSpec() {
        return createTypeSpecBuilder()
            .addMethod(applySubscriberMethodSpecUpdates(createSubscriberMethodSpecBuilder()).build())
            .addMethod(applyBuildMethodSpecUpdates(createBuildMethodSpecBuilder()).build())
            .build();
    }

    /**
     * Hook to create the {@link TypeSpec} builder implementation.
     */
    protected TypeSpec.Builder createTypeSpecBuilder() {
        ParameterizedTypeName superBuilderInterface = ParameterizedTypeName.get(
            ClassName.get(EventStreamResponseHandler.class).nestedClass("Builder"),
            responsePojoType, eventStreamUtils.eventStreamBaseClass(), className());
        return PoetUtils.createInterfaceBuilder(className()).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addJavadoc("Builder for {@link $1T}. This can be used to create the {@link $1T} in a more "
                                    + "functional way, you may also directly implement the {@link $1T} interface if "
                                    + "preferred.",
                                    responseHandlerType)
                        .addSuperinterface(superBuilderInterface);
    }

    /**
     * Hook to customize the 'subscriber' method before building the {@link MethodSpec}.
     *
     * @param builder Builder to update.
     */
    protected MethodSpec.Builder applySubscriberMethodSpecUpdates(MethodSpec.Builder builder) {
        String javadocs = new DocumentationBuilder()
            .description("Sets the subscriber to the {@link $T} of events. The given "
                         + "{@link $T} will be called for each event received by the "
                         + "publisher. Events are requested sequentially after each event is "
                         + "processed. If you need more control over the backpressure strategy "
                         + "consider using {@link #subscriber($T)} instead.")
            .param("visitor", "Visitor that will be invoked for each incoming event.")
            .returns("This builder for method chaining")
            .build();
        ClassName visitorInterface = eventStreamUtils.responseHandlerVisitorType();
        return builder.addModifiers(Modifier.ABSTRACT)
                      .addJavadoc(javadocs,
                                  Publisher.class, visitorInterface, Supplier.class);
    }

    /**
     * Creates the {@link MethodSpec.Builder} for the 'subscriber' method that may be customized by subclass.
     */
    private MethodSpec.Builder createSubscriberMethodSpecBuilder() {
        ClassName visitorInterface = eventStreamUtils.responseHandlerVisitorType();
        return MethodSpec.methodBuilder("subscriber")
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(ParameterSpec.builder(visitorInterface, "visitor")
                                                    .build())
                         .returns(eventStreamUtils.responseHandlerBuilderType());
    }

    /**
     * Hook to customize the 'build' method before building the {@link MethodSpec}.
     *
     * @param builder Builder to update.
     */
    protected MethodSpec.Builder applyBuildMethodSpecUpdates(MethodSpec.Builder builder) {
        return builder.addModifiers(Modifier.ABSTRACT)
                      .addJavadoc("@return A {@link $T} implementation that can be used in the $L API call.",
                                  responseHandlerType, eventStreamUtils.getApiName());
    }

    /**
     * Creates the {@link MethodSpec.Builder} for the 'build' method that may be customized by subclass.
     */
    private MethodSpec.Builder createBuildMethodSpecBuilder() {
        return MethodSpec.methodBuilder("build")
                         .addModifiers(Modifier.PUBLIC)
                         .returns(responseHandlerType);
    }

    @Override
    public ClassName className() {
        return eventStreamUtils.responseHandlerBuilderType();
    }
}
