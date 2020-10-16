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

import com.squareup.javapoet.ClassName;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

public final class EventStreamSpecHelper {
    private final ShapeModel eventStream;
    private final IntermediateModel intermediateModel;
    private final PoetExtensions poetExtensions;

    public EventStreamSpecHelper(ShapeModel eventStream, IntermediateModel intermediateModel) {
        this.eventStream = eventStream;
        this.intermediateModel = intermediateModel;
        this.poetExtensions = new PoetExtensions(intermediateModel);
    }

    public String visitMethodName(MemberModel event) {
        if (useLegacyGenerationScheme(event)) {
            return "visit";
        }
        return "visit" + CodegenNamingUtils.pascalCase(event.getName());
    }

    public String eventPackageName() {
        return intermediateModel.getMetadata().getFullModelPackageName() + "."
                + eventStream.getShapeName().toLowerCase(Locale.ENGLISH);
    }

    public boolean useLegacyGenerationScheme(MemberModel event) {
        Map<String, List<String>> useLegacyEventGenerationScheme = intermediateModel.getCustomizationConfig()
                .getUseLegacyEventGenerationScheme();

        List<String> targetEvents = useLegacyEventGenerationScheme.get(eventStream.getC2jName());

        if (targetEvents == null) {
            return false;
        }

        return targetEvents.stream().anyMatch(e -> e.equals(event.getC2jName()));
    }

    public ClassName eventClassName(MemberModel eventModel) {
        if (useLegacyGenerationScheme(eventModel)) {
            return poetExtensions.getModelClass(eventModel.getShape().getShapeName());
        }
        String simpleName = "Default" + CodegenNamingUtils.pascalCase(eventModel.getName());
        return ClassName.get(eventPackageName(), simpleName);
    }
}
