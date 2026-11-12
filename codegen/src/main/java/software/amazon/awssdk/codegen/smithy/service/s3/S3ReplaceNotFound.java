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

package software.amazon.awssdk.codegen.smithy.service.s3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Has HeadBucket and HeadObject throw {@code NoSuchBucket} and {@code NoSuchKey} instead of {@code NotFound}.
 */
@SdkInternalApi
final class S3ReplaceNotFound extends AbstractS3Customization {
    private static final ShapeId NOT_FOUND = s3("NotFound");

    @Override
    public Model preprocess(Model model, CustomizationConfig config) {
        List<Shape> replaced = new ArrayList<>();
        replaceNotFound(model, "HeadBucket", "NoSuchBucket").ifPresent(replaced::add);
        replaceNotFound(model, "HeadObject", "NoSuchKey").ifPresent(replaced::add);
        return ModelTransformer.create().replaceShapes(model, replaced);
    }

    /**
     * Does nothing when the operation does not list {@code NotFound}.
     */
    private static Optional<OperationShape> replaceNotFound(Model model, String operation, String error) {
        return model.getShape(s3(operation))
                    .flatMap(Shape::asOperationShape)
                    .filter(op -> op.getErrors().contains(NOT_FOUND))
                    .map(op -> op.toBuilder()
                                 .errors(op.getErrors().stream()
                                           .map(e -> e.equals(NOT_FOUND) ? s3(error) : e)
                                           .collect(Collectors.toList()))
                                 .build());
    }
}
