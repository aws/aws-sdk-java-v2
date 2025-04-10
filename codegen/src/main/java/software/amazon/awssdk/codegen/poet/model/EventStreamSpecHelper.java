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
import com.squareup.javapoet.TypeSpec;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.eventstream.EventTypeEnumSpec;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

public final class EventStreamSpecHelper {
    private final ShapeModel eventStream;
    private final IntermediateModel intermediateModel;
    private final PoetExtension poetExtensions;

    public EventStreamSpecHelper(ShapeModel eventStream, IntermediateModel intermediateModel) {
        this.eventStream = eventStream;
        this.intermediateModel = intermediateModel;
        this.poetExtensions = new PoetExtension(intermediateModel);
    }

    public String visitMethodName(MemberModel event) {
        if (legacyEventGenerationMode() == CustomizationConfig.LegacyEventGenerationMode.NO_ES_EVENT_IMPL) {
            return "visit";
        }
        return "visit" + CodegenNamingUtils.pascalCase(event.getName());
    }

    public String eventPackageName() {
        return intermediateModel.getMetadata().getFullModelPackageName() + "."
                + eventStream.getShapeName().toLowerCase(Locale.ENGLISH);
    }

    public CustomizationConfig.LegacyEventGenerationMode legacyEventGenerationMode() {
        Map<String, CustomizationConfig.LegacyEventGenerationMode> useLegacyEventGenerationScheme =
            intermediateModel.getCustomizationConfig().getUseLegacyEventGenerationScheme();

        return Optional.ofNullable(useLegacyEventGenerationScheme.get(eventStream.getC2jName()))
                       .orElse(CustomizationConfig.LegacyEventGenerationMode.DISABLED);
    }

    public ClassName eventClassName(MemberModel eventModel) {
        if (legacyEventGenerationMode() == CustomizationConfig.LegacyEventGenerationMode.NO_ES_EVENT_IMPL) {
            return poetExtensions.getModelClass(eventModel.getShape().getShapeName());
        }
        String simpleName = "Default" + intermediateModel.getNamingStrategy().getShapeClassName(eventModel.getName());
        return ClassName.get(eventPackageName(), simpleName);
    }

    public ClassName eventTypeEnumClassName() {
        return poetExtensions.getModelClass(eventStream.getShapeName()).nestedClass("EventType");
    }

    public TypeSpec eventTypeEnumSpec() {
        return new EventTypeEnumSpec("", intermediateModel, eventStream)
                .poetSpec()
                .toBuilder()
                .addModifiers(PUBLIC, Modifier.STATIC)
                .build();
    }

    public String eventTypeEnumValue(MemberModel eventModel) {
        NamingStrategy namingStrategy = intermediateModel.getNamingStrategy();
        return namingStrategy.getEnumValueName(eventModel.getName());
    }

    public String eventBuilderMethodName(MemberModel eventModel) {
        return String.format("%sBuilder", StringUtils.uncapitalize(eventModel.getName()));
    }

    public String eventConsumerName(MemberModel eventModel) {
        if (legacyEventGenerationMode() == CustomizationConfig.LegacyEventGenerationMode.NO_ES_EVENT_IMPL) {
            return "on" + eventModel.getShape().getShapeName();
        }
        return "on" + eventModel.getName();
    }
}
