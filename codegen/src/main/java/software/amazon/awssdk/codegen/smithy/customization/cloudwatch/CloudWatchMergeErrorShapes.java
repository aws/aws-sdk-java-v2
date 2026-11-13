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

package software.amazon.awssdk.codegen.smithy.customization.cloudwatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.smithy.customization.SmithyServiceCustomization;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Each pair of error shapes below becomes one exception class, registered under one shape's error code. Points the
 * error references at that shape and removes the other:
 * <ul>
 *     <li>{@code LimitExceededException} becomes {@code LimitExceededFault}.</li>
 *     <li>{@code ResourceNotFound} becomes {@code ResourceNotFoundException}.</li>
 * </ul>
 */
@SdkInternalApi
final class CloudWatchMergeErrorShapes implements SmithyServiceCustomization {
    private static final String NAMESPACE = "com.amazonaws.cloudwatch";
    private static final Map<ShapeId, ShapeId> MERGED = merged();

    @Override
    public Set<String> sdkIds() {
        return Collections.singleton("CloudWatch");
    }

    @Override
    public Model preprocess(Model model, CustomizationConfig config) {
        Map<ShapeId, ShapeId> merged = new LinkedHashMap<>();
        MERGED.forEach((from, to) -> {
            if (model.getShape(from).isPresent()) {
                if (!model.getShape(to).isPresent()) {
                    throw new IllegalStateException(to + " is not in the model, so " + from
                                                    + " cannot be merged into it");
                }
                merged.put(from, to);
            }
        });
        List<Shape> replaced = new ArrayList<>();
        for (OperationShape operation : model.getOperationShapes()) {
            if (operation.getErrors().stream().anyMatch(merged::containsKey)) {
                Set<ShapeId> errors = new LinkedHashSet<>();
                operation.getErrors().forEach(e -> errors.add(merged.getOrDefault(e, e)));
                replaced.add(operation.toBuilder().clearErrors().addErrors(errors).build());
            }
        }
        Model retargeted = ModelTransformer.create().replaceShapes(model, replaced);
        List<Shape> removed = new ArrayList<>();
        for (ShapeId from : merged.keySet()) {
            failIfReferenced(retargeted, from);
            removed.add(retargeted.expectShape(from));
        }
        return ModelTransformer.create().removeShapes(retargeted, removed);
    }

    private static void failIfReferenced(Model model, ShapeId shape) {
        for (MemberShape member : model.getMemberShapes()) {
            if (member.getTarget().equals(shape)) {
                throw new IllegalStateException(member.getId() + " targets " + shape + ", which is being removed");
            }
        }
        for (ServiceShape service : model.getServiceShapes()) {
            if (service.getErrors().contains(shape)) {
                throw new IllegalStateException(service.getId() + " lists " + shape + ", which is being removed");
            }
        }
    }

    private static Map<ShapeId, ShapeId> merged() {
        Map<ShapeId, ShapeId> merged = new LinkedHashMap<>();
        merged.put(cloudWatch("LimitExceededException"), cloudWatch("LimitExceededFault"));
        merged.put(cloudWatch("ResourceNotFound"), cloudWatch("ResourceNotFoundException"));
        return Collections.unmodifiableMap(merged);
    }

    private static ShapeId cloudWatch(String name) {
        return ShapeId.fromParts(NAMESPACE, name);
    }
}
