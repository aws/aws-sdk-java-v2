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

package software.amazon.awssdk.codegen.smithy.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * {@code LimitExceededException} and {@code LimitExceededFault} both become the Java class
 * {@code LimitExceededException}, which the shipped C2J client registers for {@code LimitExceededFault}. Error
 * references to the former point at the latter.
 */
@SdkInternalApi
final class CloudWatchCustomization implements SmithyServiceCustomization {
    private static final ShapeId FROM = ShapeId.from("com.amazonaws.cloudwatch#LimitExceededException");
    private static final ShapeId TO = ShapeId.from("com.amazonaws.cloudwatch#LimitExceededFault");

    @Override
    public Set<String> sdkIds() {
        return Collections.singleton("CloudWatch");
    }

    @Override
    public Model preprocess(Model model, CustomizationConfig config) {
        List<Shape> changed = new ArrayList<>();
        for (OperationShape operation : model.getOperationShapes()) {
            if (operation.getErrors().contains(FROM)) {
                Set<ShapeId> errors = new LinkedHashSet<>();
                operation.getErrors().forEach(e -> errors.add(e.equals(FROM) ? TO : e));
                changed.add(operation.toBuilder().clearErrors().addErrors(errors).build());
            }
        }
        Model replaced = ModelTransformer.create().replaceShapes(model, changed);
        return ModelTransformer.create().removeShapes(replaced, Collections.singletonList(replaced.expectShape(FROM)));
    }
}
