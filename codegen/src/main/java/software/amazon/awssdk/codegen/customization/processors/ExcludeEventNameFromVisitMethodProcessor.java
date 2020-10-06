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

package software.amazon.awssdk.codegen.customization.processors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;

/**
 * This process enforces constraints placed on the "excludeEventNameFromVisitMethod"; i.e. that no two members
 * of the same event stream sharing the same shape have this customization enabled for them. This processor does not
 * modify the the service or intermediate model.
 */
public class ExcludeEventNameFromVisitMethodProcessor implements CodegenCustomizationProcessor {
    private static final String CUSTOMIZATION_NAME = "ExcludeEventNameFromVisitMethod";
    private static final Logger log = LoggerFactory.getLogger(ExcludeEventNameFromVisitMethodProcessor.class);

    @Override
    public void preprocess(ServiceModel serviceModel) {
        // no-op
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        Map<String, List<String>> excludeEventNameFromVisitMethod = intermediateModel.getCustomizationConfig()
                .getExcludeEventNameFromVisitMethod();

        excludeEventNameFromVisitMethod.forEach((eventStream, members) -> {
            ShapeModel shapeModel = getShapeByC2jName(intermediateModel, eventStream);

            if (shapeModel == null || !shapeModel.isEventStream()) {
                log.warn("Encountered " + CUSTOMIZATION_NAME + " for unrecognized eventstream " + eventStream);
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
                    String msg = String.format("Encountered %s customization for unrecognized eventstream member %s#%s",
                            CUSTOMIZATION_NAME, eventStream, m);
                    log.warn(msg);
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
