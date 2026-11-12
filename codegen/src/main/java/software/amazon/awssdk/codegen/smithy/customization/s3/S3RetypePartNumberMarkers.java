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

package software.amazon.awssdk.codegen.smithy.customization.s3;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Makes {@code PartNumberMarker} and {@code NextPartNumberMarker} integers. They are pagination tokens, which Smithy
 * requires to be strings.
 */
@SdkInternalApi
final class S3RetypePartNumberMarkers extends AbstractS3Customization {
    private static final List<ShapeId> MARKERS = Arrays.asList(s3("PartNumberMarker"), s3("NextPartNumberMarker"));

    @Override
    public Model preprocess(Model model, CustomizationConfig config) {
        List<Shape> replaced = MARKERS.stream()
                                      .map(id -> integerShape(model.expectShape(id)))
                                      .collect(Collectors.toList());
        return ModelTransformer.create().replaceShapes(model, replaced);
    }

    private static IntegerShape integerShape(Shape shape) {
        return IntegerShape.builder().id(shape.getId()).addTraits(shape.getAllTraits().values()).build();
    }
}
