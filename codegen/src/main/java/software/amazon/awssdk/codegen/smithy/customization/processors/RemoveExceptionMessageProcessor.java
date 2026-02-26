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

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.smithy.customization.SmithyCustomizationProcessor;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Smithy equivalent of {@link software.amazon.awssdk.codegen.customization.processors.RemoveExceptionMessagePropertyProcessor}.
 *
 * <p>This is a Category A (postprocess-only) processor. It removes the {@code message} member
 * from all exception shapes in the IntermediateModel. Every exception class extends
 * {@code SdkException} and the {@code message} member is inherited from that class.
 */
public final class RemoveExceptionMessageProcessor implements SmithyCustomizationProcessor {

    private static final boolean IGNORE_CASE = true;

    @Override
    public Model preprocess(Model model, ServiceShape service) {
        return model;
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        for (ShapeModel shapeModel : intermediateModel.getShapes().values()) {
            if (ShapeType.Exception == shapeModel.getShapeType()) {
                shapeModel.removeMemberByC2jName("message", IGNORE_CASE);
            }
        }
    }
}
