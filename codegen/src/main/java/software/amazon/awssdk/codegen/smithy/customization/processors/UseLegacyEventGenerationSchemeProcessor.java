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

package software.amazon.awssdk.codegen.smithy.customization.processors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.smithy.customization.SmithyCustomizationProcessor;
import software.amazon.awssdk.utils.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Smithy equivalent of {@link software.amazon.awssdk.codegen.customization.processors.UseLegacyEventGenerationSchemeProcessor}.
 *
 * <p>This is a Category A (postprocess-only) processor. It enforces constraints on the
 * "UseLegacyEventGenerationScheme" customization: no two members of the same event stream
 * sharing the same shape may have this customization enabled. This processor does not modify
 * the service or intermediate model.
 */
public final class UseLegacyEventGenerationSchemeProcessor implements SmithyCustomizationProcessor {

    private static final String CUSTOMIZATION_NAME = "UseLegacyEventGenerationScheme";
    private static final Logger log = Logger.loggerFor(UseLegacyEventGenerationSchemeProcessor.class);

    @Override
    public Model preprocess(Model model, ServiceShape service) {
        return model;
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        Map<String, List<String>> useLegacyEventGenerationScheme = intermediateModel.getCustomizationConfig()
                .getUseLegacyEventGenerationScheme();

        useLegacyEventGenerationScheme.forEach((eventStream, members) -> {
            ShapeModel shapeModel = getShapeByC2jName(intermediateModel, eventStream);

            if (shapeModel == null || !shapeModel.isEventStream()) {
                log.warn(() -> String.format("Encountered %s for unrecognized eventstream %s",
                        CUSTOMIZATION_NAME, eventStream));
                return;
            }

            Map<String, Integer> shapeToEventCount = new HashMap<>();

            members.forEach(m -> {
                MemberModel event = shapeModel.getMemberByC2jName(m);

                if (event != null) {
                    String shapeName = event.getC2jShape();
                    int count = shapeToEventCount.getOrDefault(shapeName, 0);
                    shapeToEventCount.put(shapeName, ++count);
                } else {
                    log.warn(() -> String.format("Encountered %s customization for unrecognized eventstream member %s#%s",
                            CUSTOMIZATION_NAME, eventStream, m));
                }
            });

            shapeToEventCount.forEach((shape, count) -> {
                if (count > 1) {
                    throw new IllegalArgumentException(CUSTOMIZATION_NAME + " customization declared for "
                            + eventStream + ", but more than it targets more than one member with the shape " + shape);
                }
            });
        });
    }

    private ShapeModel getShapeByC2jName(IntermediateModel intermediateModel, String c2jName) {
        return intermediateModel.getShapes().values().stream()
                .filter(s -> s.getC2jName().equals(c2jName))
                .findAny()
                .orElse(null);
    }
}
